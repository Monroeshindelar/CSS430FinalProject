
public class FileSystem {
    private SuperBlock superblock;
    private Directory dir;
    private FileTable fileTable;

    public FileSystem(int diskBlocks) {
        superblock = new SuperBlock(diskBlocks);
        dir = new Directory(32); //superblock.inodeBlocks ? what is this?
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
        return null;
    }

    boolean close(FileTableEntry ftEnt) {
        return false;
    }

    int fsize(FileTableEntry ftEnt) {
        return -1;
    }

    int read(FileTableEntry ftEnt, byte[] buffer) {
        return -1;
    }

    int write(FileTableEntry ftEnt, byte[] buffer) {
        return -1;
    }

    private boolean deallocAllBlocks(FileTableEntry ftEnt) {
        return false;
    }

    boolean delete(String fileName) {
        return false;
    }

    private final int SEEK_SET = 0;
    private final int SEEK_CUR = 1;
    private final int SEEK_END = 2;

    int seek(FileTableEntry ftEnt, int offset, int whence) {
        return -1;
    }
}