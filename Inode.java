/*
@author Mark Belyak
@author Dan Florescu
@author Monroe Shindelar

File System
CSS 430 Final Project
3/10/18

Each inode will descrivbe one file. Our inode will include 12 pointers of the index block. The first 11 pointers point
to the direct bloc. The last pointer points to an indirect block. Alse, each inode must include the length of the
corressponding file, the number of file (structure) table entries that point to this inode and the flag to
indicate if it is unused (0) or used (1) or in some other status. 16 inodes can be stored in one block.
 */
public class Inode {
    private final static int iNodeSize = 32;
    public final static int directSize = 11;

    public int length; //filesize
    public short count; //# of fds referencing this inode
    public short flag; //file being used 0-unused 1-used
    public short direct[] = new short[directSize];
    public short indirect;

    /*
    Default constructor.
     */
    Inode() {
        length = 0;
        count = 0;
        flag = 1;
        for(int i = 0; i < directSize; i++) direct[i] = -1;
        indirect = -1;
    }

    /*
    Constructor that retrieves an existing inode from the disk into memory. Given an inode number.
    We will read the corressponding disk block and locate the corressponding inode information in that block and
    initialize a new inode with this information.
    */
    Inode(short iNumber) {
        //read data from disk
        byte[] buffer = new byte[Disk.blockSize];
        SysLib.rawread(1 + (iNumber / 16), buffer);

        //set offset
        int offset = iNumber % 16 * iNodeSize;

        //read length count and flag from byte array
        length = SysLib.bytes2int(buffer, offset);
        offset += 4;
        count = SysLib.bytes2short(buffer, offset);
        offset += 2;
        flag = SysLib.bytes2short(buffer, offset);
        offset += 2;

        //loop through direct block and write to array
        for(int i = 0; i < directSize; i++) {
            direct[i] = SysLib.bytes2short(buffer, offset);
            offset += 2;
        }

        //get indirect block
        indirect = SysLib.bytes2short(buffer, offset);
    }

    /*
    This method will save the inode information to the iNumber-th inode in the disk
    where iNumber is passed as an arguement.
     */
    int toDisk(short iNumber) {
        int blockNumber = 1 + iNumber / 16;
        byte [] data = new byte[Disk.blockSize];
        SysLib.rawread( blockNumber, data );
        int offset = ( iNumber % 16 ) * 32;

        // fill the temp data block with the Inode data
        SysLib.int2bytes(length, data, offset);
        offset += 4;
        SysLib.short2bytes(count, data, offset);
        offset += 2;
        SysLib.short2bytes(flag, data, offset);
        offset += 2;
        for(int i = 0; i < directSize; i++){
            SysLib.short2bytes(direct[i], data, offset);
            offset += 2;
        }
        SysLib.short2bytes(indirect, data, offset);

        // write this Inode to the disk
        return SysLib.rawwrite(blockNumber, data);
    }

    /*
    This method is responsible for changing the value of the indirect block (1st level of indirection)
    in an inode. If the conditions are correct and the index block can be set, the method allocates a new byte buffer,
    fills it with short pointers that hold -1, and writes it to the disk.
    */
    boolean setIndexBlock(short indexBlockNumber) {
        //if any direct blocks are empty return false
        for(int i = 0; i < direct.length; i++) if(direct[i] == -1) return false;

        //if indirect is not empty return false
        if(indirect != -1) return false;

        //change value of indirect block
        byte[] buffer = new byte[Disk.blockSize];
        indirect = indexBlockNumber;

        //write to disk
        for(int i = 0; i < 256; i++) SysLib.short2bytes((short)-1, buffer, i*2);
        SysLib.rawwrite(indexBlockNumber, buffer);
        return true;

    }

    /*
    The findTarget method is responsible for taking in an offset
    and returning the block for that offset. It calculates the offset
    and then searches the current inodes list of blocks (both direct and indirect)
    and returns the block.
     */
    short findTargetBlock(int offset) {
        //find block
        int block = offset / Disk.blockSize;

        //if valid block return it
        if(block < 11) return direct[block];
        //if invalid return -1
        else if(indirect < 0) return -1;
        else {
            //read indirect block and return it
            byte[] buffer = new byte[Disk.blockSize];
            SysLib.rawread(indirect, buffer);
            return SysLib.bytes2short(buffer, (block - 11) * 2);
        }
    }

    /*
    freeIndrectBlocks will remove an Inodes indirect reference and
    return the value of the indirect block.
     */
    byte[] freeIndirectBlock() {
        //if indirect exists
        if (indirect >= 0) {
            //read indirect block data in byte array
            byte[] data = new byte[512];
            SysLib.rawread(indirect, data);

            //set indirect to -1
            indirect = -1;

            //return data from indirect
            return data;
        } else return null;
    }

    /*
    The findblock method operates much like the findTargetBlock method,
    but it returns a status depending on the block access instead of the block number.
     */
    int findBlock(int seekptr, short newBlock) {
        //find block
        int targetBlock = seekptr / Disk.blockSize;

        //if target block is in direct
        if (targetBlock < directSize) {

            //if block exists in direct return -1
            if (direct[targetBlock] >= 0) return FileSystem.NOT_FREE;

            //if the block before it isn't full return -2
            else if (targetBlock > 0 && direct[targetBlock - 1] == -1 ) return FileSystem.BAD_DIRECT_ACCESS;

            //write to direct
            else {
                direct[targetBlock] = newBlock;
                return FileSystem.OK;
            }
        }
        //if indirect doesn't exist return -3
        else if (indirect < 0) return FileSystem.BAD_INDIRECT_ACCESS;


        else {
            //read indirect block data
            byte[] data = new byte[Disk.blockSize];
            SysLib.rawread(indirect, data);
            int temp = (targetBlock - directSize) * 2;

            //if data already written return -1
            if (SysLib.bytes2short(data, temp) > 0) {
                return FileSystem.NOT_FREE;

            //write data to disk and return 0
            } else {
                SysLib.short2bytes(newBlock, data, temp);
                SysLib.rawwrite(indirect, data);
                return FileSystem.OK;
            }
        }
    }
}


