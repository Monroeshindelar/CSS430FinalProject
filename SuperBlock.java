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
            SysLib.int2bytes(0,buffer,0);
            freeList = SysLib.bytes2int(buffer, 0);
            SysLib.rawwrite(iNumber, buffer);
        }
        return iNumber;
    }

    public boolean returnBlock(int blockNumber) {
        if(blockNumber < 0) return false;

        byte[] buffer = new byte[Disk.blockSize];
        for(int i = 0; i < buffer.length; i++) buffer[i] = 0;
        SysLib.int2bytes(blockNumber, buffer, 0);
        SysLib.rawwrite(blockNumber, buffer);
        freeList = blockNumber;
        return true;
    }

}
