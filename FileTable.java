import java.util.Vector;

public class FileTable {
    private Vector table;
    private Directory dir;

    public FileTable(Directory directory) {
        table = new Vector();
        dir = directory;
    }

    public synchronized FileTableEntry falloc(String fileName, String mode) {
        return null;
    }

    public synchronized boolean ffree(FileTableEntry e) {
        return false;
    }

    public synchronized boolean fempty() {
        return table.isEmpty();
    }
}
