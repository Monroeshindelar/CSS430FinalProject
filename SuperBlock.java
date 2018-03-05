class SuperBlock {
    private final int BLOCK_SIZE = 512;
    private final int defaultInodeBlocks = 64;
    public int totalBlocks;
    public int totalInodes;
    public int freeList;

    public SuperBlock(int diskSize) {
        byte[] sb = new byte[BLOCK_SIZE];
        SysLib.rawread(0, sb);
        totalBlocks = SysLib.bytes2int(sb, 0);
        totalInodes = SysLib.bytes2int(sb, 4);
        freeList = SysLib.bytes2int(sb, 8);

        if(totalBlocks == diskSize && totalInodes > 0 && freeList >= 2) return;
        else {
            totalBlocks = diskSize;
            format(defaultInodeBlocks);
        }
    }

    void sync() {
        byte[] buffer = new byte[BLOCK_SIZE];
        SysLib.int2bytes(totalBlocks, buffer, 0);
        SysLib.int2bytes(totalInodes, buffer, 4);
        SysLib.int2bytes(freeList, buffer, 8);
        SysLib.rawwrite(0, buffer);
    }

    public void format(int files) {

    }

    public int getFreeBlock() {
        int iNumber = freeList;
        if(iNumber != -1) {
            byte[] buffer = new byte[BLOCK_SIZE];
            SysLib.rawread(iNumber,buffer);
            SysLib.int2bytes(0,buffer,0);
            freeList = SysLib.bytes2int(buffer, 0);
            SysLib.rawwrite(iNumber, buffer);
        }
        return iNumber;
    }

    public boolean returnBlock(int blockNumber) {
        if(blockNumber < 0) return false;

        byte[] buffer = new byte[BLOCK_SIZE];
        for(int i = 0; i < buffer.length; i++) buffer[i] = 0;
        SysLib.int2bytes(blockNumber, buffer, 0);
        SysLib.rawwrite(blockNumber, buffer);
        freeList = blockNumber;
        return true;
    }

}
