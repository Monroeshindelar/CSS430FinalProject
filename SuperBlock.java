class SuperBlock {
    public int totalBlocks;
    public int totalInodes;
    public int freeList;

    public SuperBlock(int diskSize) {
        byte[] sb = new byte[512];
        SysLib.rawread(0, sb);
        totalBlocks = SysLib.bytes2int(sb, 0);
        totalInodes = SysLib.bytes2int(sb, 4);
        freeList = SysLib.bytes2int(sb, 8);
    }

    void sync() {
        byte[] buffer = new byte[512];
        SysLib.int2bytes(totalBlocks, buffer, 0);
        SysLib.int2bytes(totalInodes, buffer, 4);
        SysLib.int2bytes(freeList, buffer, 8);
        SysLib.rawwrite(0, buffer);
    }
}
