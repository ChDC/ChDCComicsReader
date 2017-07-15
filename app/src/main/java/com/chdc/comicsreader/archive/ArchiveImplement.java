package com.chdc.comicsreader.archive;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Created by Wen on 2017/7/15.
 */

public abstract class ArchiveImplement {

    public abstract String getName(String url);

    /*********************************** For Archive Only *****************************/

    /**
     * 根据 URL 获取实现
     * @param url
     * @return
     */
    public static ArchiveImplement getArchiveImplementByFileName(String name){
        if(name.endsWith(".rar"))
            return RARImplement.INSTANCE;
        return null;
    }

    public abstract Archive createArchive(String file);


    public List<FileHeader> listFiles(Archive archive, Pattern fileFilter){
        List<FileHeader> files =  archive.listFiles();
        if(fileFilter != null){
            for(FileHeader fh : files){
                if(!fileFilter.matcher(ArchiveImplement.this.getName(fh.getPath())).find())
                    files.remove(fh);
            }
        }
        return files;
    }

}
