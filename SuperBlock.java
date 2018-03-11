/*
@author Mark Belyak
@author Dan Florescu
@author Monroe Shindelar

File System
CSS 430 Final Project
3/10/18

The super block is bascially just a block of meta data that describes the file system and its components.
It wil read the physical super block from the disk and will validate the disk thus proving ways to free blocks,
add blocks to the freelist, and write the contents of the super block back to the disk. If the validation has failed
then it will format the disk and write a new super block to the disk.
 */
class SuperBlock {
    private final int defaultInodeBlocks = 64;
    public int totalBlocks;
    public int totalInodes;
    public int freeList;

    /*
    This constructor will read the super block from the disk and will initialize member variables for the number
    of inodes, number of blocks, tand the block number of the free list head.
     */
    public SuperBlock(int diskSize) {
        //create buffer
        byte[] sb = new byte[Disk.blockSize];

        //read totalblocks, totalinodes and free list from file to buffer
        SysLib.rawread(0, sb);
        totalBlocks = SysLib.bytes2int(sb, 0);
        totalInodes = SysLib.bytes2int(sb, 4);
        freeList = SysLib.bytes2int(sb, 8);

        if(totalBlocks != diskSize || totalInodes <= 0 || freeList < 2) {
            totalBlocks = diskSize;
            format(defaultInodeBlocks);
        }
    }

    /*
   This method will take the super block contents in line with any updates preformed to the super block class instance.
   Then we will write back to the disk the total number of blocks and the total number of inodes and the free list.
    */
    void sync() {
        //create byte buffer
        byte[] buffer = new byte[Disk.blockSize];

        //write totalblocks, totalinodes and freelist to buffer
        SysLib.int2bytes(totalBlocks, buffer, 0);
        SysLib.int2bytes(totalInodes, buffer, 4);
        SysLib.int2bytes(freeList, buffer, 8);

        //write buffer to disk
        SysLib.rawwrite(0, buffer);
    }

    /*
      This method will clear out the disk data and reformat the structure if the super block detects an improper state
      when an instance is initialized. The super block instance variables are cleared to default values and written back
      to the cleared disk.
     */
    public void format(int files) {
        totalInodes = files;

        //create new inodes and write to disk
        for(short i = 0; i < totalInodes; i++) {
            Inode current = new Inode();
            current.flag = 0;
            current.toDisk(i);
        }

        //create free list
        freeList = 2 + totalInodes * 32 / Disk.blockSize;

        //loop through free list
        for(int i = freeList; i < totalBlocks; i++) {

            //create buffer and set ever value to 0
            byte[] buffer = new byte[Disk.blockSize];
            for(int j = 0; j < Disk.blockSize; j++) buffer[j] = 0;

            //write to disk
            SysLib.int2bytes(i+1, buffer, 0);
            SysLib.rawwrite(i, buffer);
        }
        sync();
    }

    /*
    This method will return the first free block from the free list. The free block will be the top block from
    the free queue and is returned as an int value. We will return -1 if if there is an absence of free blocks.
     */
    public int getFreeBlock() {
        //get block in free list
        int iNumber = freeList;

        //if freelist exists
        if(iNumber != -1) {
            //read from disk
            byte[] buffer = new byte[Disk.blockSize];
            SysLib.rawread(iNumber,buffer);

            //write to disk
            freeList = SysLib.bytes2int(buffer, 0);
            SysLib.int2bytes(0,buffer,0);
            SysLib.rawwrite(iNumber, buffer);
        }
        return iNumber;
    }

    /*
    This method will add the newly freed block back to the free list.
    If the freed block doesnt conform to the disk parameters help in the super block then we will return false;
     */
    public boolean returnBlock(int blockNumber) {
        // Block number outside of boudns
        if(blockNumber < 0 ||  blockNumber > totalBlocks)
        {
            return false;
        }

        // temporary store data
        byte[] data = new byte[Disk.blockSize];

        //Clear data
        for(int i = 0; i < data.length; i++)
        {
            // clear each byte
            data[i] = (byte)0;
        }

        // write to list
        SysLib.int2bytes(freeList, data,0);

        // write to disk
        SysLib.rawwrite(blockNumber, data);

        //Freelist head is now parameter block number
        freeList = blockNumber;
        return true;
    }

}
