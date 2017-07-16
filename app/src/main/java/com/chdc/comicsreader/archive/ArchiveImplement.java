package com.chdc.comicsreader.archive;

import java.util.Iterator;
import java.util.List;
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

    public abstract Archive createArchive(String file, String password) throws PasswordIsWrongException;


    public List<FileHeader> listFiles(Archive archive, Pattern fileFilter){
        if(archive == null)
            return null;
        List<FileHeader> files =  archive.listFiles();
        if(fileFilter != null){
            Iterator<FileHeader> it = files.iterator();
            while(it.hasNext()){
                if(!fileFilter.matcher(ArchiveImplement.this.getName(it.next().getPath())).find())
                    it.remove();
            }
        }
        return files;
    }

}
