import java.util.Vector;
/*
@author Mark Belyak
@author Dan Florescu
@author Monroe Shindelar

File System
CSS 430 Final Project
3/10/18

The file structure table will represent the set of file table entries.
Each of the file table entry represents one file descriptor. We will create a new file table entry when its required
and we will add it to the vector table. We will remove the file table entry from the vector table when its freed.
 */

public class FileTable {
    public final static int UNUSED = 0;
    public final static int USED = 1;
    public final static int READ = 2;
    public final static int WRITE = 3;


    private Vector table;
    private Directory dir;
    /*
        This constructor will set the directory to passed directory reference and will
        create a vector fo file structure table.
         */
    public FileTable(Directory directory) {
        table = new Vector();
        dir = directory;
    }

    /*
     This method will allocate a new file (structure table entry for a file name.
     Then we will retrieve/allocate and register the corresponding inode using dir.
     The we will increment the inodes count.
     And we will immediately write back this inode to the disk and then at the end,
     we will return a reference to this file (structure) table entry.
     */
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

                //if flag is used or being read
                if (inode.flag != UNUSED && inode.flag != WRITE) {

                    inode.flag = (short)(inode.flag + 3);
                    inode.toDisk(iNumber);

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

    /*
        This method will recieve a file table entry reference and then we will save the corresponding inode to the disk.
        After that we will free this file table entry.
        We will return true if this file table entry is found in my table.
    */
    public synchronized boolean ffree(FileTableEntry ftEnt) {
        //if element is found and removed
        if (table.removeElement(ftEnt)) {

            //decrenent inode count
            ftEnt.inode.count--;

            //if flag is used/read set to unused
            if (ftEnt.inode.flag == USED || ftEnt.inode.flag == READ) ftEnt.inode.flag = UNUSED;
            //if file is 4 or 5 set to write
            else if (ftEnt.inode.flag == 4 || ftEnt.inode.flag == 5) ftEnt.inode.flag = WRITE;

            //save node to disk
            ftEnt.inode.toDisk(ftEnt.iNumber);

            //wake up other threads
            notify();
            return true;
        } else return false;
    }
}
