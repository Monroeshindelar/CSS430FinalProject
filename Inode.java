public class Inode {
    private final static int iNodeSize = 32;
    private final static int directSize = 11;

    public int length;
    public short count;
    public short flag;
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

    }

    int toDisk(short iNumber) {
        return -1;
    }

}


