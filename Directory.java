public class Directory {
    private static int maxChars = 30;

    private int fsize[];
    private char fnames[][];

    public Directory(int maxInumber) {
        fsize = new int[maxInumber];
        for(int i = 0; i < maxInumber; i++) fsize[i] = 0;
        fnames = new char[maxInumber][maxChars];
        String root = "/";
        fsize[0] = root.length();
        root.getChars(0, fsize[0], fnames[0], 0);
    }

    public void bytes2directory(byte data[]) {

    }

    public byte[] directory2bytes() {

    }

    public short ialloc(String filename) {

    }

    public boolean ifree(short iNumber) {

    }




}
