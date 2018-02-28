class SuperBlock {
    public int totalBlocks;
    public int totalInodes;
    public int freeList;

    public SuperBlock(int diskSize) {
        totalBlocks = diskSize / 512;

    }
}
