package com.chdc.comicsreader.fs;

import java.io.InputStream;
import java.util.regex.Pattern;

/**
 * Created by Wen on 2017/7/8.
 */

public abstract class FileImplement {

    /**
     * 获取父亲
     * @param url
     * @return
     */
    public abstract String getParent(String url);

    /**
     * 获取孩子
     * @param url
     * @param fileFilter 过滤文件的模式
     * @param dirBlockFilter 目录黑名单
     * @param archivePattern 压缩文件
     * @return 含有三个元素的数组，以此是 文件，目录，压缩文件
     */
    public abstract String[][] getChildren(String url, Pattern fileFilter, Pattern dirBlockFilter, Pattern archivePattern);

    /**
     * 获取文件名
     * @param url
     * @return
     */
    public abstract String getName(String url);

    /**
     * 删除文件
     * @param url
     * @param filter
     * @param recursion
     * @param deleteEmptyDirectory
     * @return
     */
    public abstract boolean delete(String url, Pattern filter, boolean recursion, boolean deleteEmptyDirectory);

    /**
     * 重命名路径
     * @param src
     * @param dest
     * @return
     */
    public abstract boolean rename(String src, String dest);

    /**
     * 文件是否为空
     * @param url
     * @return
     */
    public abstract boolean isEmpty(String url);

    /**
     * 获取文件流
     * @param url
     * @return
     */
    public abstract InputStream getInputStream(String url);

    /**
     * 根据 URL 获取实现
     * @param url
     * @return
     */
    public static FileImplement getFileImplementByURLType(String url){
        if(url == null)
            return null;
        if(url.startsWith("/"))
            return LocalFileImplement.INSTANCE;
        return null;
    }

    /**
     * 文件是否存在
     * @param url
     * @return
     */
    public abstract boolean exists(String url);

    /**
     * 将文件缓存到本地
     * @param url
     * @return
     */
    public abstract java.io.File cacheFile(String url);

    /**
     * 获取路径相关的根目录
     * @param url
     * @return
     */
    public abstract String getRootPath(String url);
}
