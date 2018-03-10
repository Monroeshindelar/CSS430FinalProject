class SuperBlock {
    private final int defaultInodeBlocks = 64;
    public int totalBlocks;
    public int totalInodes;
    public int freeList;

    public SuperBlock(int diskSize) {
        byte[] sb = new byte[Disk.blockSize];
        SysLib.rawread(0, sb);
        totalBlocks = SysLib.bytes2int(sb, 0);
        totalInodes = SysLib.bytes2int(sb, 4);
        freeList = SysLib.bytes2int(sb, 8);

        if(totalBlocks != diskSize || totalInodes <= 0 || freeList < 2) {
            totalBlocks = diskSize;
            format(defaultInodeBlocks);
        }
    }

    void sync() {
        byte[] buffer = new byte[Disk.blockSize];
        SysLib.int2bytes(totalBlocks, buffer, 0);
        SysLib.int2bytes(totalInodes, buffer, 4);
        SysLib.int2bytes(freeList, buffer, 8);
        SysLib.rawwrite(0, buffer);
    }

    public void format(int files) {
        totalInodes = files;
        for(short i = 0; i < totalInodes; i++) {
            Inode current = new Inode();
            current.flag = 0;
            current.toDisk(i);
        }
        freeList = 2 + totalInodes * 32 / Disk.blockSize;
        for(int i = freeList; i < totalBlocks; i++) {
            byte[] buffer = new byte[Disk.blockSize];
            for(int j = 0; j < Disk.blockSize; j++) buffer[j] = 0;
            SysLib.int2bytes(i+1, buffer, 0);
            SysLib.rawwrite(i, buffer);
        }
        sync();
    }

    public int getFreeBlock() {
        int iNumber = freeList;
        if(iNumber != -1) {
            byte[] buffer = new byte[Disk.blockSize];
            SysLib.rawread(iNumber,buffer);
            freeList = SysLib.bytes2int(buffer, 0);
            SysLib.int2bytes(0,buffer,0);

            SysLib.rawwrite(iNumber, buffer);
        }
        return iNumber;


    }

    public boolean returnBlock(int blockNumber) {
        if(blockNumber < 0 ||  blockNumber > totalBlocks) // Block number outside of boudns
        {
            return false;
        }
        byte[] data = new byte[Disk.blockSize];           // temporary store data
        for(int i = 0; i < data.length; i++) //Clear data
        {
            data[i] = (byte)0;                            // clear each byte
        }
        SysLib.int2bytes(freeList, data,0);           // write to list
        SysLib.rawwrite(blockNumber, data);               // write to disk
        freeList = blockNumber; //Freelist head is now parameter block number
        return true;
    }

}
