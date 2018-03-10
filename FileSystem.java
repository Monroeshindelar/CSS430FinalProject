
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
        if(ftEnt == null) return false;
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
//        if(ftEnt.mode == "w" || ftEnt.mode == "a") return -1;
//        int count = 0;
//        int size = buffer.length;
//        int bytesRead = 0;
//        synchronized(ftEnt) {
//            while(size > 0 && ftEnt.seekPtr < fsize(ftEnt)) {
//                int current = ftEnt.inode.findTargetBlock(ftEnt.seekPtr);
//                if(current == -1) break;
//
//                byte[] currentBlock = new byte[Disk.blockSize];
//                SysLib.rawread(ftEnt.iNumber, currentBlock);
//
//                int offset = ftEnt.seekPtr % Disk.blockSize;
//                int blocksRemaining = Disk.blockSize - bytesRead;
//                int fileRemaining = fsize(ftEnt) - ftEnt.seekPtr;
//
//                bytesRead = Math.min(((blocksRemaining < fileRemaining) ? blocksRemaining : fileRemaining), size);
//                System.arraycopy(currentBlock, offset, buffer, count, bytesRead);
//                count += bytesRead;
//                ftEnt.seekPtr += bytesRead;
//                size -= bytesRead;
//            }
//            SysLib.cerr("Number of Bytes Read: " + count + "\n");
//            return count;
//        }

        if (ftEnt.mode != "w" && ftEnt.mode != "a") {
            int var3 = 0;
            int var4 = buffer.length;
            synchronized(ftEnt) {
                while(var4 > 0 && ftEnt.seekPtr < this.fsize(ftEnt)) {
                    int var6 = ftEnt.inode.findTargetBlock(ftEnt.seekPtr);
                    if (var6 == -1) {
                        break;
                    }

                    byte[] var7 = new byte[512];
                    SysLib.rawread(var6, var7);
                    int var8 = ftEnt.seekPtr % 512;
                    int var9 = 512 - var8;
                    int var10 = this.fsize(ftEnt) - ftEnt.seekPtr;
                    int var11 = Math.min(Math.min(var9, var4), var10);
                    System.arraycopy(var7, var8, buffer, var3, var11);
                    ftEnt.seekPtr += var11;
                    var3 += var11;
                    var4 -= var11;
                }

                return var3;
            }
        } else {
            return -1;
        }
    }

    int write(FileTableEntry ftEnt, byte[] buffer) {
        //get size of buffer
        int bufferSize = buffer.length;

        int numberBytesWritten = 0;

        //check that mode is correct
        if (ftEnt.mode != "r")
        {
            synchronized (ftEnt)
            {
                while(bufferSize > 0)
                {
                    int currentBlock = ftEnt.inode.findTargetBlock(ftEnt.seekPtr);

                    //if block doesn't exist
                    if (currentBlock == -1)
                    {
                        short newBlock = (short) superblock.getFreeBlock();

                        int tempSeekPtr = ftEnt.inode.findBlock(ftEnt.seekPtr, newBlock);
                        if (tempSeekPtr == -3)
                        {
                            short freeBlock = (short) superblock.getFreeBlock();
                            if (!ftEnt.inode.setIndexBlock(freeBlock))
                            {
                                return -1;
                            }

                            if (ftEnt.inode.findBlock(ftEnt.seekPtr,newBlock) != 0)
                            {
                                return -1;
                            }
                        }
                        else if (tempSeekPtr == -2 || tempSeekPtr == -1)
                        {
                            return -1;
                        }
                        currentBlock = newBlock;

                    }
                    byte[] temp = new byte[Disk.blockSize];
                    int tempPtr = ftEnt.seekPtr % Disk.blockSize;
                    int numBytes = Math.min((Disk.blockSize-tempPtr), bufferSize);
                    System.arraycopy(buffer,numberBytesWritten,temp,tempPtr,numBytes );
                    SysLib.rawwrite(currentBlock, temp);


                    ftEnt.seekPtr += numBytes;
                    numberBytesWritten += numBytes;
                    bufferSize -= numBytes;
                    if (ftEnt.seekPtr > ftEnt.inode.length)
                    {
                        ftEnt.inode.length = ftEnt.seekPtr;
                    }
                }
                ftEnt.inode.toDisk(ftEnt.iNumber);
                return numberBytesWritten;
            }
        }
        else
        {
            return -1;
        }

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
        FileTableEntry ftEnt = open(fileName, "w");

        short temp = ftEnt.iNumber;

        if(close(ftEnt) && dir.ifree(temp))
        {
            return true;
        }

        return false;
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