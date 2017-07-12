package com.chdc.comicsreader.book;

import android.support.v4.provider.DocumentFile;

import com.chdc.comicsreader.utils.ViewHelper;

import org.w3c.dom.Document;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.File;
import java.util.regex.Pattern;

/**
 * Created by Wen on 2017/7/8.
 */

public class LocalFileImplement extends FileImplement {

    private static final String TAG = "LocalFileImplement";

    private static final String DELETE_TAG = "_deleted";

    public static final FileImplement INSTANCE = new LocalFileImplement();

    private String[] sdcardPaths;

    private LocalFileImplement(){ }

    @Override
    public String getParent(String url) {
//        Log.d(TAG, "getParent");
        return new File(url).getParent();
    }

    @Override
    public String[] getDirectories(String url, Pattern pattern) {
//        Log.d(TAG, "getDirectories");
        File file = new File(url);
        try {
            File[] files;
            if(pattern != null)
                files = file.listFiles(f -> f.isDirectory() && !f.getName().endsWith(DELETE_TAG) && pattern.matcher(f.getName()).find());
            else
                files = file.listFiles(f -> f.isDirectory() && !f.getName().endsWith(DELETE_TAG));
//            if(files == null)
//                return new String[0];
            String[] result = new String[files.length];
            for(int i = 0; i < result.length; i++)
                result[i] = files[i].toString();
            return result;
        }
        catch (Exception e){
            return new String[0];
        }
    }

    @Override
    public String[] getFiles(String url, final Pattern pattern) {
//        Log.d(TAG, "getFiles");
        File file = new File(url);
        try {
            File[] files;
            if(pattern != null)
                files = file.listFiles(f -> f.isFile() && pattern.matcher(f.getName()).find());
            else
                files = file.listFiles(File::isFile);
//            if(files == null)
//                return new String[0];
            String[] result = new String[files.length];
            for(int i = 0; i < result.length; i++)
                result[i] = files[i].toString();
            return result;
        }
        catch (Exception e){
            return new String[0];
        }
    }

    /**
     * 先根遍历
     * @param visitor
     * @param reversed
     */
    protected boolean behindOrderTraverseFile(File file, Visitor visitor){
        File[] children = file.listFiles();
        if(children == null)
            return true;
        for(File f : children){
            boolean willContinue;
            if(!f.isDirectory())
                willContinue = visitor.visit(f);
            else
                willContinue = behindOrderTraverseFile(f, visitor);
            if(!willContinue)
                return false;
        }
        return visitor.visit(file);
    }

    /**
     * 先根遍历
     * @param visitor
     * @param reversed
     */
    protected boolean behindOrderTraverseDocumentFile(DocumentFile file, DocumentFileVisitor visitor){
        DocumentFile[] children = file.listFiles();
        if(children == null)
            return true;
        for(DocumentFile f : children){
            boolean willContinue;
            if(!f.isDirectory())
                willContinue = visitor.visit(f);
            else
                willContinue = behindOrderTraverseDocumentFile(f, visitor);
            if(!willContinue)
                return false;
        }
        return visitor.visit(file);
    }

    protected interface Visitor {
        boolean visit(File file);
    }

    protected interface DocumentFileVisitor {
        boolean visit(DocumentFile file);
    }

    @Override
    public boolean isEmpty(String url){
        File file = new File(url);
        if(file.isFile())
            return file.length() <= 0;
        else if(file.isDirectory()){
            String[] children = file.list();
            return children == null || children.length <= 0;
        }
        else
            return !file.exists();
    }

    private boolean deleteDirectory(String url, Pattern filter, boolean recursion, boolean deleteEmptyDirectory){
        File file = new File(url);
        if(!file.isDirectory())
            return false;
        url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        File deleteFile = new File(url + DELETE_TAG);
        if(recursion) {
            // 递归
            boolean result = file.renameTo(deleteFile);
            if (result)
                new Thread(() -> {
                    // 递归删除文件和目录
                    // 后根遍历
                    behindOrderTraverseFile(deleteFile, f -> {
                        if(f.isDirectory()) {
                            if (deleteEmptyDirectory)
                                f.delete();
                        }
                        else if(filter.matcher(f.getName()).find())
                            f.delete();
                        return true;
                    });
                }).start();
            return result;
        }
        else{
            // 看文件成分
            File[] children = file.listFiles();
            boolean hasDirectory = false;
            for(File f : children){
                if(f.isDirectory()) {
                    hasDirectory = true;
                    break;
                }
            }
            if(hasDirectory){
                // 直接删除
                for(File f : children){
                    if(f.isFile() && filter.matcher(f.getName()).find())
                        f.delete();
                }
                return true;
            }
            else{
                // 只有文件，改名字删除
                boolean result = file.renameTo(deleteFile);
                if (result)
                    new Thread(() -> {
                        File[] fs = deleteFile.listFiles(f -> f.isFile() && filter.matcher(f.getName()).find());
                        for(File f : fs)
                            f.delete();
                        if(deleteEmptyDirectory)
                            deleteFile.delete(); // 如果不为空则删除失败
                        // 将名字改回来
                        deleteFile.renameTo(file);
                    }).start();
                return result;
            }
        }
    }


    private boolean deleteDocumentDirectory(DocumentFile file, Pattern filter, boolean recursion, boolean deleteEmptyDirectory){
        if(!file.isDirectory())
            return false;
        String originName = file.getName();
        String deleteFile = originName + DELETE_TAG;
        if(recursion) {
            // 递归
            boolean result = file.renameTo(deleteFile);
            if (result)
                new Thread(() -> {
                    // 递归删除文件和目录
                    // 后根遍历
                    behindOrderTraverseDocumentFile(file, f -> {
                        if(f.isDirectory()) {
                            if (deleteEmptyDirectory)
                                f.delete();
                        }
                        else if(filter.matcher(f.getName()).find())
                            f.delete();
                        return true;
                    });
                }).start();
            return result;
        }
        else{
            // 看文件成分
            DocumentFile[] children = file.listFiles();
            boolean hasDirectory = false;
            for(DocumentFile f : children){
                if(f.isDirectory()) {
                    hasDirectory = true;
                    break;
                }
            }
            if(hasDirectory){
                // 直接删除
                boolean result = true;
                for(DocumentFile f : children){
                    if(f.isFile() && filter.matcher(f.getName()).find())
                        if(!f.delete())
                            result = false;
                }
                if(deleteEmptyDirectory)
                    file.delete(); // 如果不为空则删除失败
                return result;
            }
            else{
                // 只有文件，改名字删除
                boolean result = file.renameTo(deleteFile);
                if (result)
                    new Thread(() -> {
                        DocumentFile[] fs = file.listFiles();
                        for(DocumentFile f : fs)
                            if(f.isFile() && filter.matcher(f.getName()).find())
                                f.delete();
                        if(deleteEmptyDirectory)
                            file.delete(); // 如果不为空则删除失败
                        // 将名字改回来
                        file.renameTo(originName);
                    }).start();
                return result;
            }
        }
    }

    private boolean deleteDocumentFile(File url, Pattern filter, boolean recursion, boolean deleteEmptyDirectory){
        DocumentFile file = ViewHelper.INSTANCE.getDocumentFile(url, url.isDirectory());
        if(file == null)
            return false;
        boolean result = false;
        if(file.isFile()) {
            // delete file
            result = file.delete();
            if(deleteEmptyDirectory) // && isEmpty(file.getParent()))
                file.getParentFile().delete();// 如果不为空则删除失败
        }
        else if(file.isDirectory())
            result = deleteDocumentDirectory( file, filter, recursion, deleteEmptyDirectory);
        return result;
    }

    @Override
    public boolean delete(String url, Pattern filter, boolean recursion, boolean deleteEmptyDirectory) {
        boolean result = false;
        File file = new File(url);
        if(ViewHelper.INSTANCE.includeByExternalSDCard(url))
            return deleteDocumentFile(file, filter, recursion, deleteEmptyDirectory);
        if(file.isFile()) {
            // delete file
            result = file.delete();
            if(deleteEmptyDirectory) // && isEmpty(file.getParent()))
                new File(file.getParent()).delete();// 如果不为空则删除失败
        }
        else if(file.isDirectory())
            result = deleteDirectory(url, filter, recursion, deleteEmptyDirectory);
        return result;
    }

    @Override
    public boolean rename(String src, String dest) {
        return new File(src).renameTo(new File(dest));
    }

    @Override
    public InputStream getInputStream(String url) {
//        Log.d(TAG, "getInputStream");
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
        return new File(url).exists();
    }

    @Override
    public File cacheFile(String url) {
        return new File(url);
    }

    @Override
    public String getRootPath(String url) {
        if(sdcardPaths == null){
            sdcardPaths = new String[2];
            sdcardPaths[0] = ViewHelper.INSTANCE.getInnerSDCardPath();
            sdcardPaths[1] = ViewHelper.INSTANCE.getExternalSDCardPath();
        }
        if(url == null)
            return sdcardPaths[0];

        for (String sdcardPath : sdcardPaths) {
            if (url.startsWith(sdcardPath))
                return sdcardPath;
        }
        return null;
    }
}
