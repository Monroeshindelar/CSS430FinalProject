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
//        short iNumber = -1;
//        Inode inode = null;
//
//        while (true) {
//            //get inode number from file
//            iNumber = fileName.equals("/") ? 0 : dir.namei(fileName);
//
//            // check if file exists
//            if (iNumber >= 0) {
//                inode = new Inode(iNumber);
//                // if mode is read
//                if (mode.equals("r")) {
//                    //if file is being written to
//                    if (inode.flag != 0 || inode.flag != 1 ) {
//                        //wait until writing is done
//                        try {
//                            wait();
//                        }
//                        catch (InterruptedException e) { }
//                        continue;
//                    }
//                    //set flag to read
//                    inode.flag = 1;
//                    break;
//                }
//                else {
//                    if (inode.flag == 0 || inode.flag == 1) {
//                        inode.flag = 3;
//                        break;
//                    }
//                    //wait until writing is done
//                    else {
//                        try {
//                            wait();
//                        }
//                        catch (InterruptedException e) { }
//                    }
//                }
//            }
//            //if mode is read then return a null
//            if (mode.equals("r")) return null;
//
//            //create file
//            iNumber = dir.ialloc(fileName);
//            inode = new Inode(iNumber);
//            //set flag to write
//            inode.flag = 3;
//            break;
//        }
//
//        //write inode to disk
//        inode.count++;
//        inode.toDisk(iNumber);
//        //create filetable entry and return
//        FileTableEntry ftEnt = new FileTableEntry(inode, iNumber, mode);
//        table.addElement(ftEnt);
//        return ftEnt;
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
