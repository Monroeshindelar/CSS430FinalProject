import java.util.Vector;

public class FileTable {
    private Vector table;
    private Directory dir;

    public FileTable(Directory directory) {
        table = new Vector();
        dir = directory;
    }

    public synchronized FileTableEntry falloc(String fileName, String mode) {
        short nodeNum = -1;
        Inode inode = null;

        while (true)
        {
            //get inode number from file
            if (fileName.equals("/"))
            {
                nodeNum = 0;
            }
            else
            {
                nodeNum = dir.namei(fileName);
            }

            // check if file exists
            if (nodeNum >= 0)
            {

                inode = new Inode(nodeNum);

                // if mode is read
                if (mode.equals("r"))
                {
                    //if file is being written to
                    if (inode.flag != 0 || inode.flag != 1 )
                    {
                        //wait until writing is done
                        try
                        {
                            wait();
                        }
                        catch (InterruptedException e) { }
                        continue;
                    }
                    //set flag to read
                    inode.flag = 1;
                    break;
                }
                else
                {
                    if (inode.flag == 0 || inode.flag == 1)
                    {
                        inode.flag = 3;
                        break;
                    }
                    //wait until writing is done
                    else
                    {
                        try
                        {
                            wait();
                        }
                        catch (InterruptedException e) { }
                    }
                }
            }
            //if mode is read then return a null
            if (mode.equals("r"))
            {
                return null;
            }

            //create file
            nodeNum = dir.ialloc(fileName);
            inode = new Inode(nodeNum);
            //set flag to write
            inode.flag = 3;
            break;
        }

        //write inode to disk
        inode.count++;
        inode.toDisk(nodeNum);
        //create filetable entry and return
        FileTableEntry ftEnt = new FileTableEntry(inode, nodeNum, mode);
        table.addElement(ftEnt);
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
