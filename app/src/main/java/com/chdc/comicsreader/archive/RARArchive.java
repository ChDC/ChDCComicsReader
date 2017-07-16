package com.chdc.comicsreader.archive;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import de.innosystec.unrar.Archive;
import de.innosystec.unrar.rarfile.FileHeader;

/**
 * Created by Wen on 2017/7/15.
 */

public class RARArchive implements com.chdc.comicsreader.archive.Archive {

    private Archive rarFile;

    public RARArchive(String file, String password) throws Exception{
        rarFile = new Archive(new File(file), password, false);
        if(rarFile.isEncrypted() && !rarFile.isPass())
            throw new PasswordIsWrongException();
    }

    @Override
    public synchronized boolean extractFile(com.chdc.comicsreader.archive.FileHeader fileHeader, OutputStream outputStream) throws PasswordIsWrongException {
        if(!(fileHeader instanceof RARFileHeader) || outputStream == null)
            return false;
        RARFileHeader rarFileHeader = (RARFileHeader)fileHeader;
        try {
            rarFile.extractFile(rarFileHeader.getFileHeader(), outputStream);
        } catch (Throwable e) {
            if(e instanceof java.lang.OutOfMemoryError)
                System.gc();
            else if(rarFileHeader.getFileHeader().isEncrypted())
                throw new PasswordIsWrongException();
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public synchronized List<com.chdc.comicsreader.archive.FileHeader> listFiles() {
        List<com.chdc.comicsreader.archive.FileHeader> list = new ArrayList<>();
        for(FileHeader entry = rarFile.nextFileHeader(); entry != null; entry = rarFile.nextFileHeader()){
            if(entry.isDirectory())
                continue;
            String entryPath = entry.isUnicode() ? entry.getFileNameW().trim() : entry.getFileNameString().trim();
            entryPath = entryPath.replace("\\\\", "/");
            if(!entryPath.contains("/") && entryPath.contains("\\"))
                entryPath = entryPath.replace("\\", "/");
            list.add(new RARFileHeader(entryPath, entry));
        }
        return list;
    }

    @Override
    public void close() throws IOException {
        rarFile.close();
    }
}
