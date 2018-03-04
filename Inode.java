public class Inode {
    private final static int iNodeSize = 32;
    private final static int directSize = 11;

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
        byte[] buffer = new byte[32];
        SysLib.rawread(iNumber, buffer);
        length = SysLib.bytes2int(buffer, 0);
        count = SysLib.bytes2short(buffer, 4);
        flag = SysLib.bytes2short(buffer, 6);
        int offset = 8;
        for(int i = 0; i < directSize; i++) {
            direct[i] = SysLib.bytes2short(buffer, offset);
            offset += 2;
        }
        indirect = SysLib.bytes2short(buffer, offset);
    }

    int toDisk(short iNumber) {
        byte[] buffer = new byte[32];
        SysLib.int2bytes(length, buffer, 0);
        SysLib.short2bytes(count, buffer, 4);
        SysLib.short2bytes(flag, buffer, 6);
        int offset = 8;
        for(int i = 0; i < directSize; i++) {
            SysLib.short2bytes(direct[i], buffer, offset);
            offset = offset + 2;
        }
        SysLib.short2bytes(indirect, buffer, offset);
        return SysLib.rawwrite(iNumber, buffer);
    }

    short getIndexBlockNumber() {
        return indirect;
    }

    boolean setIndexBlock(short indexBlockNumber) {
        if(indirect != -1) return false;
        for(int i = 0; i < direct.length; i++) if(direct[i] == -1) return false;
        byte[] buffer = new byte[512];
        for(int i = 0; i < (buffer.length) / 2; i++) SysLib.short2bytes((short)-1, buffer, i*2);
        SysLib.rawwrite(indexBlockNumber, buffer);
        return true;
    }

    short findTargetBlock(int offset) {
        int block = 512 / offset;
        if(block < 0) return -1;
        else if(block < 11) return direct[block];
        else {
            byte[] buffer = new byte[512];
            SysLib.rawread(indirect, buffer);
            return SysLib.bytes2short(buffer, (block - 11) * 2);
        }
    }
}


