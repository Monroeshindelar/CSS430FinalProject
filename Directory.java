/*
@author Mark Belyak
@author Dan Florescu
@author Monroe Shindelar

File System
CSS 430 Final Project
3/10/18

The "/" root directory maintains each file in a different directory entry that contains
its file name and the corresponding inode number.
The directory will receive the max number of inodes to be created, bascially the max number fo files to be created
and keeps track of which inode numbers are in use.
Since the directory itself is considered as a file, its contents are maintained by an inode,
specifically saying inode 0. This can be located in the first 32 bytes of the disk block 1.
 */
public class Directory {
    private static int maxChars = 30; // max characters of each file name

    private int fsize[];// each element stores a different file size.
    private char fnames[][];// each element stores a different file name.

    /*
    Directory constructor.
     */
    public Directory(int maxInumber) {  // directory constructor
        fsize = new int[maxInumber];    // maxInumber = max files
        for(int i = 0; i < maxInumber; i++) fsize[i] = 0;   // all file size initialized to 0
        fnames = new char[maxInumber][maxChars];
        String root = "/";          // entry(inode) 0 is "/"
        fsize[0] = root.length();   // fsize[0] is the size of "/".
        root.getChars(0, fsize[0], fnames[0], 0);
    }

    /*
    We will assumes that the data byte array passed recieved the directory information from the disk.
    Thus, we will then initialize the directory instance with this data byte array.
     */
    public void bytes2directory(byte data[]) {
        int offset = 0;

        //create file size array from data array
        for(int i = 0; i < fsize.length; i++) {
            fsize[i] = SysLib.bytes2int(data, offset);
            offset += 4;
        }

        //create file name array from data array
        for(int i = 0; i < fnames.length; i++) {
            String names = new String(data, offset, maxChars * 2);
            names.getChars(0, fsize[i], fnames[i], 0);
            offset += maxChars * 2;
        }

    }

    /*
    This method will convert meaningful directory information into a plain byte array.
    The byte array will then be written back to the disk.
     */
    public byte[] directory2bytes() {
        //create buffer to store bytes
        byte[] buffer = new byte[(fsize.length * 4) + ((maxChars * fnames.length) * 2)];
        int offset = 0;

        //convert file sizes to bytes and store in buffer
        for(int i = 0; i < fsize.length; i++) {
            SysLib.int2bytes(fsize[i], buffer, offset);
            offset += 4;
        }

        //convert files names to bytes and store in buffer
        for(int i = 0; i < fnames.length; i++) {
            char[] temp = new char[maxChars];
            for(int j = 0; j < maxChars; j++) temp[j] = fnames[i][j];
            byte[] converted = new String(temp).getBytes();
            for(int j = 0; j < converted.length; j++) buffer[offset++] = converted[i];
        }
        return buffer;
    }

    /*
    The filename passed in is one of the files to be created.
    Then we will allocate a new inode number for this filename.
     */
    public short ialloc(String filename) {
        //loop through file array and find empty file
        for(short i = 0; i < fsize.length; i++) {

            //if empty file found
            if(fsize[i] == 0) {

                //allocate new inode number for this filename
                fsize[i] = (maxChars > filename.length()) ? filename.length() : maxChars;
                filename.getChars(0, fsize[i], fnames[i], 0);
                return i;
            }
        }
        return -1;
    }

    /*
    This method will deallocate the passed in iNumber(inode number).
    Then we will delete the corresponding file.
     */
    public boolean ifree(short iNumber) {
        boolean retVal = false;

        //if file exists
        if(fsize[iNumber] > 0) {
            //delete file
            fsize[iNumber] = 0;
            retVal = true;
        }
        return retVal;
    }

    /*
    This method will return a inumber corresponding to passed in file name.
     */
    public short namei(String filename) {
        //loop through file names list
        for(short i = 0; i < fnames.length; i++) {
            //if length of filename is the same as one you are searching for
            if (fsize[i] == filename.length()) {
                //check if file name found and return inumber
                String current = new String(fnames[i], 0, fsize[i]);
                if (filename.equals(current)) return i;
            }
        }

        return -1;
    }




}
