
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
        return false;
    }

    FileTableEntry open(String fileName, String mode) {
        FileTableEntry retVal = fileTable.falloc(fileName, mode);
        return retVal;
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

        return false;
    }

    boolean delete(String fileName) {
        Inode current = new Inode(dir.namei(fileName));
        if(current.count > 0) return false;
        dir.ifree(dir.namei(fileName));
        return true;
    }

    int seek(FileTableEntry ftEnt, int offset, int whence) {
        synchronized (ftEnt) {
            switch (whence) {
                case SEEK_SET:
                    ftEnt.seekPtr = offset;
                    break;
                case SEEK_CUR:
                    ftEnt.seekPtr += offset;
                    break;
                case SEEK_END:
                    ftEnt.seekPtr = ftEnt.inode.length + offset;
                    break;
                default:
                    return -1;
            }

            if (ftEnt.seekPtr < 0) ftEnt.seekPtr = 0;
            if (ftEnt.seekPtr > ftEnt.inode.length) ftEnt.seekPtr = ftEnt.inode.length;
            return ftEnt.seekPtr;
        }
    }
}