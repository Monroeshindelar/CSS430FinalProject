import java.util.Vector;

public class FileTable {
    private Vector table;
    private Directory dir;

    public FileTable(Directory directory) {
        table = new Vector();
        dir = directory;
    }

    public synchronized FileTableEntry falloc(String fileName, String mode) {
        short iNumber = fileName.equals("/") ? 0 : dir.namei(fileName);
        if(iNumber == -1) iNumber = dir.ialloc(fileName);
        byte[] buffer = new byte[Disk.blockSize];
        int blockNum;
        if(iNumber % 16 != 0) blockNum = 1 + iNumber / 16;
        else blockNum = iNumber / 16;
        SysLib.rawread(blockNum, buffer);
        Inode temp = new Inode(iNumber);
        FileTableEntry ftEnt = new FileTableEntry(temp, iNumber, mode);
        temp.count++;
        temp.toDisk(iNumber);
        table.add(ftEnt);
        return ftEnt;
    }

    public synchronized boolean ffree(FileTableEntry e) {
        boolean retVal = false;
        e.inode.count--;
        e.inode.toDisk(e.iNumber);
        int index = table.indexOf(e);
        if(index >= 0)  {
            retVal = true;
            table.set(index, null);
        }
        return retVal;
    }

    public synchronized boolean fempty() {
        return table.isEmpty();
    }
}
