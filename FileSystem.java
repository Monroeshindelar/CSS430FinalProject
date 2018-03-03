
public class FileSystem {
    private final int BLOCK_SIZE = 512;
    private SuperBlock superblock;
    private Directory dir;
    private FileTable fileTable;

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

    }

    boolean format(int files) {
        return false;
    }

    FileTableEntry open(String fileName, String mode) {
        FileTableEntry retVal = fileTable.falloc(fileName, mode);
        return retVal;
    }

    boolean close(FileTableEntry ftEnt) {
        return false;
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
                    byte[] currentBlock = new byte[BLOCK_SIZE];
                    SysLib.rawread(ftEnt.iNumber, currentBlock);

                }
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

    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;

    int seek(FileTableEntry ftEnt, int offset, int whence) {
        return -1;
    }
}