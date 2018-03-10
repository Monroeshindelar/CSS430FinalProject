import java.util.Vector;

public class FileTable {
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
                    if (inode.flag != 0 && inode.flag != 1 ) {
                        //wait until writing is done
                        try {
                            wait();
                        }
                        catch (InterruptedException e) { }
                        continue;
                    }
                    //set flag to read
                    inode.flag = 1;
                    break;
                }

                if (inode.flag == 0 && inode.flag == 3)
                {
                    if(inode.flag == 1 || inode.flag==2)
                    {
                        inode.flag = (short)(inode.flag + 3);
                        inode.toDisk(iNumber);
                    }
                    inode.flag = 3;

                    //wait until writing is done

                    try
                    {
                        wait();
                    }
                    catch (InterruptedException e) { }
                    continue;
                }
                inode.flag = 2;
                break;
            }
            //if mode is read then return a null
            if (mode.equals("r")) return null;

            //create file
            iNumber = dir.ialloc(fileName);
            inode = new Inode(iNumber);
            //set flag to write
            inode.flag = 3;
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

        if (this.table.removeElement(ftEnt)) {
            --ftEnt.inode.count;
            if (ftEnt.inode.flag == 1 || ftEnt.inode.flag == 2)
            {
                ftEnt.inode.flag = 0;
            }
            else if (ftEnt.inode.flag == 4 || ftEnt.inode.flag == 5)
            {
                ftEnt.inode.flag = 3;
            }

            ftEnt.inode.toDisk(ftEnt.iNumber);
            ftEnt = null;
            this.notify();
            return true;
        } else {
            return false;
        }
    }

    public synchronized boolean fempty() {
        return table.isEmpty();
    }
}
