/*
@author Mark Belyak
@author Dan Florescu
@author Monroe Shindelar

File System
CSS 430 Final Project
3/10/18

The file system should provide user threads with the system calls that will allow them to format,
to open, to read from , to write to, to update the seek pointer of, to close, to delete,
and to get the size of their files.
The file system being created will consist of a single level.
The "/" root directory is predefined by the file system and permanently available for user threads to store their files.
No other directories are provided by the system and created by users dynamically.
 */
public class FileSystem {
    private SuperBlock superblock;
    private Directory dir;
    private FileTable fileTable;

    public final static int ERROR = -1;
    public final static int SEEK_SET    =  0;
    public final static int SEEK_CUR    =  1;
    public final static int SEEK_END    =  2;
    public final static int NOT_FREE = -1;
    public final static int BAD_DIRECT_ACCESS = -2;
    public final static int BAD_INDIRECT_ACCESS = -3;
    public final static int OK = 0;
    /*
    This constructor will create the superblock, directory, and files table and will store the file table in the directory.
     */
    public FileSystem(int diskBlocks) {
        //initiate other classes
        superblock = new SuperBlock(diskBlocks);
        dir = new Directory(superblock.totalInodes);
        fileTable = new FileTable(dir);

        //open root directory with read mode
        FileTableEntry dirEnt = open("/", "r");
        int dirSize = fsize(dirEnt);
        if (dirSize > 0) {
            byte[] dirData = new byte[dirSize];
            read(dirEnt, dirData);
            dir.bytes2directory(dirData);
        }
        close(dirEnt);
    }

    /*
    Formats the disk (Disk.java's data contents). The parameter files specifies the maximum number of files
    to be created (the number of inodes to be allocated) in your file system.
    The return value is 0 on success, otherwise -1.
    */
    boolean format(int files) {
        //call superblock format
        superblock.format(files);

        //create new directory and file table
        dir = new Directory(superblock.totalInodes);
        fileTable = new FileTable(dir);
        return true;
    }

    /*
    This will sync the files back to the physical disk adn will write the directory information to the disk in byte
    form in the root directory. We will also make sure that the super block is also synced.
     */
    void sync() {
        //open root directory with write mode
        FileTableEntry root = open("/", "w");

        //write and close directory
        byte[] dirInBytes = dir.directory2bytes();
        write(root, dirInBytes);
        close(root);

        //call superblock sync
        superblock.sync();
    }

    /*
    This methdod passes in two parameters, a filename and a mode. We will open a file specified by the filename.
    We first start by allocating a new file table entry and once it ahs been created we will check to see if
    the mode that we passed in is was write 'w'. If it is, then we will delete all blocks and start
    writing from scratch. After this, we will return the new file table entry object/
     */
    FileTableEntry open(String fileName, String mode) {
        //allocate a new filetable entry
        FileTableEntry ftEnt = fileTable.falloc(fileName, mode);
        //if writing make sure that all blocks are deallocated
        if (mode.equals ("w")) if(!deallocAllBlocks(ftEnt)) return null;
        return ftEnt;
    }

    /*
    This method will close the file corressponding to the passed in file table entry.
    We will return true if we successfully close.
     */
    boolean close(FileTableEntry ftEnt) {
        if(ftEnt == null) return false;
        synchronized (ftEnt) {
            //decrease number of thread using this FileTableEntry
            ftEnt.count--;

            //if file is closed return true
            if(ftEnt.count > 0) return true;

            //otherwise try to remove filetableentry from file table
            return fileTable.ffree(ftEnt);
        }
    }

    /*
    Returns the size in bytes of the file indicated by fd.
     */
    int fsize(FileTableEntry ftEnt) {
        synchronized(ftEnt){ return ftEnt.inode.length; }
    }

    /*
    Reads up to buffer.length bytes from the file indicated by fd, starting at the position currently pointed to by
    the seek pointer. If bytes remaining between the current seek pointer and the end of file
    are less than buffer.length, SysLib.read reads as many bytes as possible, putting them into the beginning of buffer.
    It increments the seek pointer by the number of bytes to have been read.
    The return value is the number of bytes that have been read, or a negative value upon an error.
     */
    int read(FileTableEntry ftEnt, byte[] buffer) {
        //if mode isn't read then return error
        if(ftEnt.mode == "w" || ftEnt.mode == "a") return -1;
        int count = 0;

        //get size that you are reading
        int size = buffer.length;
        int bytesRead = 0;

        synchronized(ftEnt) {
            //loop until finished reading or run out of things to read
            while(size > 0 && ftEnt.seekPtr < fsize(ftEnt)) {
                //find block that you are reading
                int current = ftEnt.inode.findTargetBlock(ftEnt.seekPtr);

                //if not found then break
                if(current == -1) break;

                //read that block into byte array of size 512
                byte[] currentBlock = new byte[Disk.blockSize];
                SysLib.rawread(current, currentBlock);

                //get amount of blocks you have read
                int offset = ftEnt.seekPtr % Disk.blockSize;

                //get blocks and size of file remaining
                int blocksRemaining = Disk.blockSize - offset;
                int fileRemaining = fsize(ftEnt) - ftEnt.seekPtr;

                //find bytes read
                bytesRead = Math.min(((blocksRemaining < size) ? blocksRemaining : size), fileRemaining);

                System.arraycopy(currentBlock, offset, buffer, count, bytesRead);

                //increment seekptr, count and size
                ftEnt.seekPtr += bytesRead;
                count += bytesRead;
                size -= bytesRead;
            }

            //return size
            return count;
        }
    }

    /*
    This method Writes the contents of buffer to the file indicated by fd, starting at the position
    indicated by the seek pointer. The operation may overwrite existing data in the file and/or append to the end
    of the file. SysLib.write increments the seek pointer by the number of bytes to have been written.
    The return value is the number of bytes that have been written, or a negative value upon an error.
     */
    int write(FileTableEntry ftEnt, byte[] buffer) {
        //if mode is not write then return error
        if (ftEnt.mode == "r") return ERROR;
        synchronized (ftEnt) {
            int count = 0;
            //get length we are writing
            int size = buffer.length;

            //while writing is unfinished
            while (size > 0) {

                //find block we are writing too
                int current = ftEnt.inode.findTargetBlock(ftEnt.seekPtr);
                if (current == -1) {

                    //get available block from superblock
                    short freeBlock = (short) superblock.getFreeBlock();

                    //find that block on disk
                    int status = ftEnt.inode.findBlock(ftEnt.seekPtr, freeBlock);
                    if(status == BAD_INDIRECT_ACCESS) {
                            //get available block from superblock
                            short temp = (short) superblock.getFreeBlock();

                            //create indirect block
                            if(!ftEnt.inode.setIndexBlock(temp)) return ERROR;

                            //fuind the block on disk
                            if(ftEnt.inode.findBlock(ftEnt.seekPtr, freeBlock) != OK) return ERROR;
                    } else if(status == BAD_DIRECT_ACCESS || status == NOT_FREE) return ERROR;

                    //found block
                    current = freeBlock;
                }

                //read data from block
                byte[] data = new byte[Disk.blockSize];
                SysLib.rawread(current, data);

                //get amount of blocks you have read
                int offset = ftEnt.seekPtr % Disk.blockSize;

                //get blocks and size of file remaining
                int blocksRemaining = Disk.blockSize - offset;
                int bytesWritten = (blocksRemaining < size) ? blocksRemaining : size;

                //copy buffer to data array
                System.arraycopy(buffer, count, data, offset, bytesWritten);

                //write to found block
                SysLib.rawwrite(current, data);

                //increment seekptr, count and size
                ftEnt.seekPtr += bytesWritten;
                count += bytesWritten;
                size -= bytesWritten;

                //if seek ptr is larger than file then set it to end of file.
                if (ftEnt.seekPtr > ftEnt.inode.length) ftEnt.inode.length = ftEnt.seekPtr;
            }
            ftEnt.inode.toDisk(ftEnt.iNumber);
            return count;
        }
    }

    /*
    deallocate all blocks associated with a file, sets all direct and inderect vlocks to null
     */
    private boolean deallocAllBlocks(FileTableEntry ftEnt) {
        if (ftEnt.inode.count != 1) return false;
        //loop through al inode blocks
        for (int i = 0; i < ftEnt.inode.directSize; i++) {
            //set them all to -1
            if (ftEnt.inode.direct[i] != -1) {
               superblock.returnBlock(i);
               ftEnt.inode.direct[i] = -1;
            }
        }

        //unregister index bloc
        byte [] tempData = ftEnt.inode.freeIndirectBlock();

        //if data is not null
        if (tempData != null) {
            short tempId;
            //loop through all valid
            while((tempId = SysLib.bytes2short(tempData,0))!= -1) return superblock.returnBlock(tempId);
        }
        //write inodes back to disk
        ftEnt.inode.toDisk(ftEnt.iNumber);
        return true;
    }

    /*
    This method deletes the file specified by fileName.
    All blocks used by file are freed.
    If the file is currently open,it isnt deleted and the operation returns false.
    If successfully deleted, we return true.
     */
    boolean delete(String fileName) {
        //find file
        FileTableEntry ftEnt = open(fileName, "w");

        short temp = ftEnt.iNumber;

        //call close and ifree
        if(close(ftEnt) && dir.ifree(temp))return true;
        return false;
    }

    /*
    Updates the seek pointer corresponding to fd as follows:
    If whence is SEEK_SET (= 0), the file's seek pointer is set to offset bytes from the beginning of the file
    If whence is SEEK_CUR (= 1), the file's seek pointer is set to its current value plus the offset. The offset can be positive or negative.
    If whence is SEEK_END (= 2), the file's seek pointer is set to the size of the file plus the offset. The offset can be positive or negative.
    If the user attempts to set the seek pointer to a negative number you must clamp it to zero.
    If the user attempts to set the pointer to beyond the file size, you must set the seek pointer to the end of the file.
    The offset location of the seek pointer in the file is returned from the call to seek.
     */
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
                    return ERROR;
            }
            //if seek ptr below 0 set to a valid seekptr
            if (ftEnt.seekPtr < 0) ftEnt.seekPtr = 0;

            //if pointer is greater than file length then set it to end of file
            if (ftEnt.seekPtr > ftEnt.inode.length) ftEnt.seekPtr = ftEnt.inode.length;
            return ftEnt.seekPtr;
        }
    }
}