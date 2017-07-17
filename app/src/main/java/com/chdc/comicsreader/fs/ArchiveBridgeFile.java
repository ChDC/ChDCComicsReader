package com.chdc.comicsreader.fs;

import com.chdc.comicsreader.archive.Archive;
import com.chdc.comicsreader.archive.ArchiveFile;
import com.chdc.comicsreader.archive.ArchiveImplement;
import com.chdc.comicsreader.archive.ArchivePage;
import com.chdc.comicsreader.archive.FileHeader;
import com.chdc.comicsreader.archive.PasswordIsWrongException;
import com.chdc.comicsreader.archive.PasswordIsWrongListener;
import com.chdc.comicsreader.book.Page;
import com.chdc.comicsreader.utils.NumberStringComparator;
import com.chdc.comicsreader.utils.Utils;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by Wen on 2017/7/14.
 */

public class ArchiveBridgeFile extends File {

    protected static PasswordIsWrongListener passwordIsWrongListener;
    private Archive archive;
    private String password;
    private ArchiveImplement archiveImplement;
    protected boolean passwordIsWrong = false;

    private static Map<String, SoftReference<ArchiveBridgeFile>> cache = new HashMap<>();
    public static ArchiveBridgeFile getArchiveBridgeFile(String url){
        ArchiveBridgeFile archiveBridgeFile;
        SoftReference<ArchiveBridgeFile> value = cache.get(url);
        if(value == null || value.get() == null){
            archiveBridgeFile = new ArchiveBridgeFile(url);
            cache.put(url, new SoftReference<>(archiveBridgeFile));
        }
        else
            archiveBridgeFile = value.get();
        return archiveBridgeFile;
    }

    private ArchiveBridgeFile(String url){
        super(url);
    }

    private ArchiveFile newArchiveFile(File parent, String path, String name){
        ArchiveFile af = new ArchiveFile(path);
        af.name = name;
        af.children = new ArrayList<>();
        af.cacheParent = true;
        af.cacheChildren = true;
        if(parent != null)
            af.parent = parent;
        return af;
    }

    private ArchivePage newArchivePage(File parent, String path, String name, Archive archive, FileHeader fileHeader){
        ArchivePage ap = new ArchivePage(path, this, archive, fileHeader);
        ap.name = name;
        ap.cacheParent = true;
        if(parent != null)
            ap.parent = parent;
        return ap;
    }

    @Override
    public List<File> getChildren(){
        if(children == null)
            buildFileTree();
        return children;
    }

    private synchronized void buildFileTree(){
        if(children != null)
            return;

        // 获取文件列表
        ArchiveImplement implement = getArchiveImplement();
        List<FileHeader> files = implement.listFiles(getArchive(), File.IMAGE_FILE_PATTERN);
        // TODO: 支持压缩文件内的压缩文件

        if(files == null)
            return;
        // 根据文件列表构建文件树
        this.children = new ArrayList<>();

        for(FileHeader fh : files){
            String[] paths = fh.getPath().split("/");
            File cd = this;
            outer:
            for(int i = 0; i < paths.length - 1; i++){
                String dirName = paths[i];
                // 搜索指定名字的子目录
                for(File f : cd.children){
                    if(f instanceof ArchivePage)
                        continue;
                    ArchiveFile af = (ArchiveFile)f;
                    if(af.name.equals(dirName)) {
                        cd = af;
                        continue outer;
//                        break;
                    }
                }
                // 没搜索到子目录，创建
                ArchiveFile af = newArchiveFile(cd, Utils.stringJoin("/", paths, 0, i + 1), dirName);
                cd.children.add(af);
                cd = af;
            }

            // 将剩下的文件填充
            String fileName = paths[paths.length - 1];
            cd.children.add(newArchivePage(cd, Utils.stringJoin("/", paths), fileName, getArchive(), fh));
        }

        sortChildren(this);
    }

    private void sortChildren(File file){
        if(file == null || file.children == null)
            return;

        // 将文件和目录分开
        List<Page> files = new ArrayList<>();
        List<File> dirs = new ArrayList<>();
        for(File f : file.children){
            if(f.isPage()) {
                Page p = (Page)f;
                p.setPageType(Page.PageType.NotEnd);
                files.add(p);
            }
            else
                dirs.add(f);
        }

        // 将文件和目录排序
        Collections.sort(files, (p1, p2) -> NumberStringComparator.INSTANCE.compare(p1.getName(), p2.getName()));
        Collections.sort(dirs, (p1, p2) -> NumberStringComparator.INSTANCE.compare(p1.getName(), p2.getName()));

        // 给文件添加类型
        if(files.size() > 0) {
            files.get(files.size() - 1).setPageType(Page.PageType.TailEnd);
            files.get(0).setPageType(Page.PageType.HeadEnd);
        }

        file.children.clear();
        file.children.addAll(files);
        file.children.addAll(dirs);

        // 递归排序目录的所有孩子
        for(File f: dirs)
            sortChildren(f);
    }

    public ArchiveImplement getArchiveImplement() {
        if(archiveImplement == null) {
            archiveImplement = ArchiveImplement.getArchiveImplementByFileName(this.getName());
            if (archiveImplement == null) {
                children = new ArrayList<>();
                return null;
            }
        }
        return archiveImplement;
    }

    public Archive getArchive() {
        if(archive == null) {
            try {
                archive = this.getArchiveImplement().createArchive(url, password);
            } catch (PasswordIsWrongException e) {
                setPasswordIsWrong(true);
                e.printStackTrace();
            }
        }
        return archive;
    }

    @Override
    protected void finalize() throws Throwable{
        if(this.archive != null)
            this.archive.close();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if(!Objects.equals(password, this.password)) {
            this.password = password;
            this.passwordIsWrong = false;
            if(archive != null)
                try {
                    archive.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            archive = null; // 重新获取文件
            this.children = null;
        }
    }

    public static PasswordIsWrongListener getPasswordIsWrongListener() {
        return passwordIsWrongListener;
    }

    public static void setPasswordIsWrongListener(PasswordIsWrongListener passwordIsWrongListener) {
        ArchiveBridgeFile.passwordIsWrongListener = passwordIsWrongListener;
    }

    public boolean isPasswordIsWrong() {
        return passwordIsWrong;
    }

    public void setPasswordIsWrong(boolean passwordIsWrong) {
        if(passwordIsWrong != this.passwordIsWrong){
            this.passwordIsWrong = passwordIsWrong;
            if(passwordIsWrong && getPasswordIsWrongListener() != null)
                getPasswordIsWrongListener().run(this);
        }
    }
}
