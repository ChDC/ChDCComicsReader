package com.chdc.comicsreader.archive;

import com.chdc.comicsreader.fs.File;
import com.chdc.comicsreader.fs.FileImplement;

import java.util.List;

/**
 * Created by Wen on 2017/7/14.
 */

public class ArchiveFile extends File {


    public ArchiveFile(String url) {
        super(url);
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
    public List<File> getChildren() {
        return children;
    }

    @Override
    public boolean delete() {
        return false; // TODO: 删除压缩文件
    }

    @Override
    public boolean isValid(){
        return true;
    }

}
