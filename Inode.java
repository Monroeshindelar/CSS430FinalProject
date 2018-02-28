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

}


