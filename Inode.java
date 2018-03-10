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
        byte[] buffer = new byte[iNodeSize];
        SysLib.int2bytes(length, buffer, 0);
        SysLib.short2bytes(count, buffer, 4);
        SysLib.short2bytes(flag, buffer, 6);
        int offset = 8;
        for(int i = 0; i < directSize; i++) {
            SysLib.short2bytes(direct[i], buffer, offset);
            offset += 2;
        }

        SysLib.short2bytes(indirect, buffer, offset);
        offset += 2;
        int test  = 1 + iNumber / 16;
        byte[] temp = new byte[512];
        SysLib.rawread(directSize, temp);
        offset = iNumber % 16 * 32;
        System.arraycopy(buffer, 0, temp, offset, iNodeSize);
        return SysLib.rawwrite(i, temp);
    }

    short getIndexBlockNumber() {
        return indirect;
    }

    boolean setIndexBlock(short indexBlockNumber) {
        if(indirect != -1) return false;
        for(int i = 0; i < direct.length; i++) if(direct[i] == -1) return false;
        byte[] buffer = new byte[Disk.blockSize];
        for(int i = 0; i < (buffer.length) / 2; i++) SysLib.short2bytes((short)-1, buffer, i*2);
        SysLib.rawwrite(indexBlockNumber, buffer);
        return true;
    }

    short findTargetBlock(int offset) {
        int block = offset / Disk.blockSize;
        if(block < 0) return -1;
        else if(block < directSize) return direct[block];
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
            else if (direct[targetBlock - 1] == -1 && targetBlock > 0)
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


