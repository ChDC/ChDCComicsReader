package com.chdc.comicsreader.archive;

import de.innosystec.unrar.rarfile.FileHeader;

/**
 * Created by Wen on 2017/7/15.
 */

public class RARFileHeader implements com.chdc.comicsreader.archive.FileHeader {

    private FileHeader fileHeader;
    private String path;

    public RARFileHeader(String path, FileHeader fileHeader){
        this.path = path;
        this.fileHeader = fileHeader;
    }

    public FileHeader getFileHeader() {
        return fileHeader;
    }

    @Override
    public String getPath() {
        return path;
    }
}
