package com.chdc.comicsreader.archive;

import com.chdc.comicsreader.book.Page;
import com.chdc.comicsreader.fs.ArchiveBridgeFile;
import com.chdc.comicsreader.fs.File;
import com.chdc.comicsreader.fs.FileImplement;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Wen on 2017/7/15.
 */

public class ArchivePage extends Page {

    protected ArchiveBridgeFile archiveBridgeFile;
    protected FileHeader fileHeader;
    protected Archive archive;
    protected java.io.File cachedFile;

    public ArchivePage(String url, ArchiveBridgeFile archiveBridgeFile, Archive archive, FileHeader fileHeader) {
        super(url);
        this.archiveBridgeFile = archiveBridgeFile;
        this.fileHeader = fileHeader;
        this.archive = archive;
    }

    @Override
    protected FileImplement getFileImplement() {
        return fileImplement;
    }

    @Override
    public File getParent(){
        return parent;
    }

    @Override
    public String getName() {
        return name;
    }

    public java.io.File getCachedFile(){
        if(cachedFile == null) {
            try {
                java.io.File tempFile = java.io.File.createTempFile("archive_", getName());
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    archive.extractFile(fileHeader, fos);
                } catch (PasswordIsWrongException e) {
                    this.archiveBridgeFile.setPasswordIsWrong(true);
                    return null;
                }
                cachedFile = tempFile;
            } catch (IOException e) {
                return null;
            }
        }
        return cachedFile;
    }

    protected InputStream getInputStream(){
        try{

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            archive.extractFile(fileHeader, bos);
            return new ByteArrayInputStream(bos.toByteArray());
        }
        catch (PasswordIsWrongException e){
            this.archiveBridgeFile.setPasswordIsWrong(true);
            return null;
        }
        catch (Exception e) {
            return null;
        }
    }

    @Override
    public void clear(){
        this.cannotLoadBitmap = false;
    }

    public boolean delete() {
        return false; // TODO: 删除压缩文件
    }

    public boolean isValid(){
        return fileHeader != null;
    }

    public ArchiveBridgeFile getArchiveBridgeFile() {
        return archiveBridgeFile;
    }
}
