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
//        int offset = 0;
//        for(int i = 0; i < fsize.length; i++) {
//            fsize[i] = SysLib.bytes2int(data, offset);
//            offset += 4;
//        }
//        for(int i = 0; i < fnames.length; i++) {
//            String names = new String(data, offset, maxChars * 2);
//            names.getChars(0, fsize[i], fnames[i], 0);
//            offset += maxChars * 2;
//        }
        int var2 = 0;

        int var3;
        for(var3 = 0; var3 < this.fsize.length; var2 += 4) {
            this.fsize[var3] = SysLib.bytes2int(data, var2);
            ++var3;
        }

        for(var3 = 0; var3 < this.fnames.length; var2 += maxChars * 2) {
            String var4 = new String(data, var2, maxChars * 2);
            var4.getChars(0, this.fsize[var3], this.fnames[var3], 0);
            ++var3;
        }

    }

    public byte[] directory2bytes() {
//        byte[] buffer = new byte[(fsize.length * 4) + ((maxChars * fnames.length) * 2)];
//        int offset = 0;
//        for(int i = 0; i < fsize.length; i++) {
//            SysLib.int2bytes(fsize[i], buffer, offset);
//            offset += 4;
//        }
//        for(int i = 0; i < fnames.length; i++) {
//            char[] temp = new char[maxChars];
//            for(int j = 0; j < maxChars; j++) temp[j] = fnames[i][j];
//            byte[] converted = new String(temp).getBytes();
//            for(int j = 0; j < converted.length; j++) buffer[offset++] = converted[i];
//        }
//        return buffer;
        byte[] var1 = new byte[this.fsize.length * 4 + this.fnames.length * maxChars * 2];
        int var2 = 0;

        int var3;
        for(var3 = 0; var3 < this.fsize.length; var2 += 4) {
            SysLib.int2bytes(this.fsize[var3], var1, var2);
            ++var3;
        }

        for(var3 = 0; var3 < this.fnames.length; var2 += maxChars * 2) {
            String var4 = new String(this.fnames[var3], 0, this.fsize[var3]);
            byte[] var5 = var4.getBytes();
            System.arraycopy(var5, 0, var1, var2, var5.length);
            ++var3;
        }

        return var1;
    }

    public short ialloc(String filename) {
        for(short i = 0; i < fsize.length; i++) {
            if(fsize[i] == 0) {
                fsize[i] = (maxChars > filename.length()) ? filename.length() : maxChars;
                filename.getChars(0, fsize[i], fnames[i], 0);
                return i;
            }
        }
        return -1;
    }

    public boolean ifree(short iNumber) {
        boolean retVal = false;
        if(fsize[iNumber] > 0) {
            fsize[iNumber] = 0;
            retVal = true;
        }
        return retVal;
    }

    public short namei(String filename) {
        for(short i = 0; i < fnames.length; i++) {
            if (fsize[i] == filename.length()) {
                String current = new String(fnames[i], 0, fsize[i]);
                if (filename.equals(current)) return i;
            }
        }

        return -1;
    }




}
