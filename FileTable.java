import java.util.Vector;

public class FileTable {
    private Vector table;
    private Directory dir;

    public FileTable(Directory directory) {
        table = new Vector();
        dir = directory;
    }

    public synchronized FileTableEntry falloc(String fileName, String mode) {
        short iNumber = dir.namei(fileName);
        FileTableEntry retVal = null;
        Inode node = null;

        if(iNumber < 0) iNumber = dir.ialloc(fileName);

        node = new Inode(iNumber);
        node.count++;

        retVal = new FileTableEntry(node, iNumber, mode);
        table.add(retVal);
        return retVal;
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
