import java.util.Vector;

public class FileTable {
    public final static int UNUSED = 0;
    public final static int USED = 1;
    public final static int READ = 2;
    public final static int WRITE = 3;


    private Vector table;
    private Directory dir;

    public FileTable(Directory directory) {
        table = new Vector();
        dir = directory;
    }

    public synchronized FileTableEntry falloc(String fileName, String mode) {
        short iNumber = -1;
        Inode inode = null;

        while (true) {
            //get inode number from file
            iNumber = fileName.equals("/") ? 0 : dir.namei(fileName);

            // check if file exists
            if (iNumber >= 0) {
                inode = new Inode(iNumber);
                // if mode is read
                if (mode.equals("r")) {
                    //if file is being written to
                    if (inode.flag != UNUSED && inode.flag != USED ) {
                        //wait until writing is done
                        try { wait(); }
                        catch (InterruptedException e) { }
                        continue;
                    }
                    //set flag to read
                    inode.flag = 1;
                    break;
                }

                if (inode.flag != UNUSED && inode.flag != WRITE) {
                    if(inode.flag == USED || inode.flag == READ) {
                        inode.flag = (short)(inode.flag + 3);
                        inode.toDisk(iNumber);
                    }
                    //wait until writing is done
                    try { wait(); }
                    catch (InterruptedException e) { }
                    continue;
                }
                inode.flag = READ;
                break;
            }
            //if mode is read then return a null
            if (mode.equals("r")) return null;
            //create file
            iNumber = dir.ialloc(fileName);
            inode = new Inode();
            //set flag to write
            inode.flag = 2;
            break;
        }

        //write inode to disk
        inode.count++;
        inode.toDisk(iNumber);
        //create filetable entry and return
        FileTableEntry ftEnt = new FileTableEntry(inode, iNumber, mode);
        table.addElement(ftEnt);
        return ftEnt;
    }

    public synchronized boolean ffree(FileTableEntry ftEnt) {
        if (table.removeElement(ftEnt)) {
            --ftEnt.inode.count;
            if (ftEnt.inode.flag == USED || ftEnt.inode.flag == READ) ftEnt.inode.flag = UNUSED;
            else if (ftEnt.inode.flag == 4 || ftEnt.inode.flag == 5) ftEnt.inode.flag = WRITE;
            ftEnt.inode.toDisk(ftEnt.iNumber);
            this.notify();
            return true;
        } else return false;
    }
}
