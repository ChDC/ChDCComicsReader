package com.chdc.comicsreader.book;

import java.io.InputStream;
import java.util.regex.Pattern;

/**
 * Created by Wen on 2017/7/8.
 */

public abstract class FileImplement {

    public abstract String getParent(String url);

    /**
     * 获取目录
     * @param url
     * @return
     */
    public abstract String[] getDirectories(String url);

    /**
     * 获取文件
     * @param url
     * @return
     */
    public abstract String[] getFiles(String url, Pattern pattern);

    public abstract boolean delete(String url);

    public abstract InputStream getInputStream(String url);

    public static FileImplement getFileImplementByURLType(String url){
        if(url.startsWith("/"))
            return LocalFileImplement.INSTANCE;
        return null;
    }

    public abstract boolean exists(String url);
}
