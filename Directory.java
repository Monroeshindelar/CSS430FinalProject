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
        int offset = 0;
        for(int i = 0; i < fsize.length; i++) {
            fsize[i] = SysLib.bytes2int(data, offset);
            offset += 4;
        }
        for(int i = 0; i < fnames.length; i++) {
            for(int j = 0; j < maxChars; j++) {
                //convert the bytes to chars
            }
        }

    }

    public byte[] directory2bytes() {
        byte[] buffer = new byte[(fsize.length * 4) + ((maxChars * fnames.length) * 2)];
        int offset = 0;
        for(int i = 0; i < fsize.length; i++) {
            SysLib.int2bytes(fsize[i], buffer, offset);
            offset += 4;
        }
        for(int i = 0; i < fnames.length; i++) {
            char[] temp = new char[maxChars];
            for(int j = 0; j < maxChars; j++) temp[j] = fnames[i][j];
            //convert char array into bytes

        }
        return buffer;
    }

    public short ialloc(String filename) {

    }

    public boolean ifree(short iNumber) {

    }




}
