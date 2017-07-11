package com.chdc.comicsreader.book;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.regex.Pattern;

/**
 * Created by Wen on 2017/7/8.
 */

public class LocalFileImplement extends FileImplement {

    public static final FileImplement INSTANCE = new LocalFileImplement();

    private LocalFileImplement(){ }

    @Override
    public String getParent(String url) {
        return new java.io.File(url).getParent();
    }

    @Override
    public String[] getDirectories(String url) {
        java.io.File file = new java.io.File(url);
        java.io.File[] files = file.listFiles(java.io.File::isDirectory);
        if(files == null)
            return new String[0];
        String[] result = new String[files.length];
        for(int i = 0; i < result.length; i++)
            result[i] = files[i].toString();
        return result;
    }

    @Override
    public String[] getFiles(String url, final Pattern pattern) {
        java.io.File file = new java.io.File(url);
        java.io.File[] files = file.listFiles(f -> f.isFile() && pattern.matcher(f.getName()).find());
        if(files == null)
            return new String[0];
        String[] result = new String[files.length];
        for(int i = 0; i < result.length; i++)
            result[i] = files[i].toString();
        return result;
    }

    @Override
    public boolean delete(String url) {
        return new File(url).delete();
    }

    @Override
    public InputStream getInputStream(String url) {
        if(url == null) return null;
        try{
            FileInputStream fis = new FileInputStream(url);
            return fis;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean exists(String url) {
        return new java.io.File(url).exists();
    }
}
