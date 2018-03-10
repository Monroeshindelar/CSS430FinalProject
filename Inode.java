public class Inode {
    private final static int iNodeSize = 32;
    public final static int directSize = 11;

    public int length; //filesize
    public short count; //# of fds referencing this inode
    public short flag; //file being used 0-unused 1-used
    public short direct[] = new short[directSize];
    public short indirect;

    Inode() {
        length = 0;
        count = 0;
        flag = 1;
        for(int i = 0; i < directSize; i++) direct[i] = -1;
        indirect = -1;
    }

    Inode(short iNumber) {
        byte[] buffer = new byte[Disk.blockSize];
        SysLib.rawread(1 + (iNumber / 16), buffer);
        int offset = iNumber % 16 * iNodeSize;
        length = SysLib.bytes2int(buffer, offset);
        offset += 4;
        count = SysLib.bytes2short(buffer, offset);
        offset += 2;
        flag = SysLib.bytes2short(buffer, offset);
        offset += 2;
        for(int i = 0; i < directSize; i++) {
            direct[i] = SysLib.bytes2short(buffer, offset);
            offset += 2;
        }
        indirect = SysLib.bytes2short(buffer, offset);
    }

    int toDisk(short iNumber) {
//        if (iNumber < 0 || iNumber >= 64)
//            return -1;
//
//        byte[] inodeData = new byte[iNodeSize]; //First we need to store 'this' inode's data into a byte array
//        int offset = 0;
//        SysLib.int2bytes(length, inodeData, offset); //Storing length as 4 bytes
//        offset += 4;
//        SysLib.short2bytes(count, inodeData, offset); //Storing count as 2 bytes
//        offset += 2;
//        SysLib.short2bytes(flag, inodeData, offset); //Storing flag as 2 bytes
//        offset += 2;
//
//        for (int i = 0; i < directSize; i++) {
//            SysLib.short2bytes(direct[i], inodeData, offset); //Storing all direct pointers as 2 bytes
//            offset += 2;
//        }
//
//        SysLib.short2bytes(indirect, inodeData, offset);
//        int blockNumber = 1 + iNumber / 16; //Address into the correct block
//        byte[] block = new byte[512];
//        SysLib.rawread(blockNumber, block);
//        offset = iNumber % 16 * 32; //Offset into the correct inode in the block
//        System.arraycopy(inodeData, 0, block, offset, 32); //Create a copy of all 32 bits from inodeData array into the block at the correct inode offset
//        SysLib.rawwrite(blockNumber, block);
//        return 0;
        byte[] var2 = new byte[32];
        byte var3 = 0;
        SysLib.int2bytes(this.length, var2, var3);
        int var6 = var3 + 4;
        SysLib.short2bytes(this.count, var2, var6);
        var6 += 2;
        SysLib.short2bytes(this.flag, var2, var6);
        var6 += 2;

        int var4;
        for(var4 = 0; var4 < 11; ++var4) {
            SysLib.short2bytes(this.direct[var4], var2, var6);
            var6 += 2;
        }

        SysLib.short2bytes(this.indirect, var2, var6);
        var6 += 2;
        var4 = 1 + iNumber / 16;
        byte[] var5 = new byte[512];
        SysLib.rawread(var4, var5);
        var6 = iNumber % 16 * 32;
        System.arraycopy(var2, 0, var5, var6, 32);
        SysLib.rawwrite(var4, var5);
        return 0;
    }

    short getIndexBlockNumber() {
        return indirect;
    }

    boolean setIndexBlock(short indexBlockNumber) {
        for(int i = 0; i < direct.length; i++) if(direct[i] == -1) return false;
        if(indirect != -1) return false;

        byte[] buffer = new byte[Disk.blockSize];
        indirect = indexBlockNumber;
        for(int i = 0; i < 256; i++) SysLib.short2bytes((short)-1, buffer, i*2);
        SysLib.rawwrite(indexBlockNumber, buffer);
        return true;

    }

    short findTargetBlock(int offset) {
        int block = offset / Disk.blockSize;
        if(block < 11) return direct[block];
        else if(indirect < 0) return -1;
        else {
            byte[] buffer = new byte[Disk.blockSize];
            SysLib.rawread(indirect, buffer);
            return SysLib.bytes2short(buffer, (block - 11) * 2);
        }
    }

    byte[] freeIndirectBlock() {
        if(indirect == -1) return null;

        byte[] buffer = new byte[Disk.blockSize];
        SysLib.rawread(indirect, buffer);
        indirect = -1;
        return buffer;

    }

    int findBlock(int seekptr, short newBlock) {
        int targetBlock = seekptr / Disk.blockSize;

        if (targetBlock < directSize)
        {
            if (direct[targetBlock] >= 0)
            {
                return -1;
            }
            else if (targetBlock > 0 && direct[targetBlock - 1] == -1 )
            {
                return -2;
            }
            else
            {
                direct[targetBlock] = newBlock;
                return 0;
            }
        }
        else if (indirect < 0)
        {
            return -3;
        }

        else
        {
            byte[] data = new byte[Disk.blockSize];
            SysLib.rawread(indirect, data);
            int temp = (targetBlock - directSize) * 2;
            if (SysLib.bytes2short(data, temp) > 0)
            {
                return -1;
            }
            else
            {
                SysLib.short2bytes(newBlock, data, temp);
                SysLib.rawwrite(this.indirect, data);
                return 0;
            }
        }

    }
}


