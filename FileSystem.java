
public class FileSystem {
    private SuperBlock superblock;
    private Directory dir;
    private FileTable fileTable;

    public final static int SEEK_SET    =  0;
    public final static int SEEK_CUR    =  1;
    public final static int SEEK_END    =  2;

    public FileSystem(int diskBlocks) {
        superblock = new SuperBlock(diskBlocks);
        dir = new Directory(superblock.totalInodes);
        fileTable = new FileTable(dir);

        FileTableEntry dirEnt = open("/", "r");
        int dirSize = fsize(dirEnt);
        if (dirSize > 0) {
            byte[] dirData = new byte[dirSize];
            read(dirEnt, dirData);
            dir.bytes2directory(dirData);
        }
        close(dirEnt);
    }

    void sync() {
        FileTableEntry root = open("/", "w");
        byte[] dirInBytes = dir.directory2bytes();
        write(root, dirInBytes);
        close(root);
        superblock.sync();
    }

    boolean format(int files) {

        superblock.format(files);

        dir = new Directory(superblock.totalInodes);

        fileTable = new FileTable(dir);

        return true;
    }

    FileTableEntry open(String fileName, String mode) {
        //allocate a new filetable entry
        FileTableEntry ftEnt = fileTable.falloc(fileName, mode);

        //if writing
        if (mode.equals ("w"))
        {
            //make sure blocks are unallocated
            if (!deallocAllBlocks(ftEnt))
            {
                //if deallocating didn't succeed then return null
                return null;
            }
        }

        return ftEnt;
    }

    boolean close(FileTableEntry ftEnt) {
        synchronized (ftEnt) {
            ftEnt.count--;
            if(ftEnt.count > 0) return true;
        }
        return fileTable.ffree(ftEnt);
    }

    int fsize(FileTableEntry ftEnt) {
        synchronized(ftEnt){ return ftEnt.inode.length; }
    }

    int read(FileTableEntry ftEnt, byte[] buffer) {
        if(ftEnt.mode == "r" || ftEnt.mode == "w+") {
            int count = 0;
            int i = buffer.length;
            synchronized(ftEnt) {
                while(i > 0 && ftEnt.seekPtr < fsize(ftEnt)) {
                    byte[] currentBlock = new byte[512];
                    SysLib.rawread(ftEnt.iNumber, currentBlock);

                }
                return count;
            }
        }
        return -1;
    }

    int write(FileTableEntry ftEnt, byte[] buffer) {
        return -1;
    }

    private boolean deallocAllBlocks(FileTableEntry ftEnt) {

        if (ftEnt.inode.count != 1)
        {
            return false;
        }

        //loop through al inode blocks
        for (int i = 0; i < ftEnt.inode.directSize; i++)
        {
            //set them all to -1
            if (ftEnt.inode.direct[i] != -1)
            {
               superblock.returnBlock(i);
               ftEnt.inode.direct[i] = -1;
            }
        }

        //
        byte [] tempData = ftEnt.inode.freeIndirectBlock();

        if (tempData != null)
        {
            short tempId;

            //loop through all valid
            while((tempId = SysLib.bytes2short(tempData,0))!= -1)
            {
                return superblock.returnBlock(tempId);
            }
        }
        //write inodes back to disk
        ftEnt.inode.toDisk(ftEnt.iNumber);
        return true;
    }

    boolean delete(String fileName) {
        //find file
        Inode current = new Inode(dir.namei(fileName));

        //if currently being used then do not delete
        if(current.count > 0) return false;

        //remove from directory
        dir.ifree(dir.namei(fileName));
        return true;
    }

    int seek(FileTableEntry ftEnt, int offset, int whence) {
        synchronized (ftEnt) {
            switch (whence) {
                //set to beginning
                case SEEK_SET:
                    ftEnt.seekPtr = offset;
                    break;

                //set to current
                case SEEK_CUR:
                    ftEnt.seekPtr += offset;
                    break;
                //set to end of file
                case SEEK_END:
                    ftEnt.seekPtr = ftEnt.inode.length + offset;
                    break;
                default:
                    return -1;
            }

            //if seek ptr below 0 set to a valid seekptr
            if (ftEnt.seekPtr < 0) ftEnt.seekPtr = 0;

            //if pointer is greater than file length then set it to end of file
            if (ftEnt.seekPtr > ftEnt.inode.length) ftEnt.seekPtr = ftEnt.inode.length;
            return ftEnt.seekPtr;
        }
    }
}