package com.chdc.comicsreader.fs;

import com.chdc.comicsreader.archive.Archive;
import com.chdc.comicsreader.archive.ArchiveFile;
import com.chdc.comicsreader.archive.ArchiveImplement;
import com.chdc.comicsreader.archive.ArchivePage;
import com.chdc.comicsreader.archive.FileHeader;
import com.chdc.comicsreader.utils.Utils;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Wen on 2017/7/14.
 */

public class ArchiveBridgeFile extends File {

    private Archive archive;
    private boolean builtFileTree = false;
    private ArchiveImplement archiveImplement;

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
        if(children == null && !builtFileTree) {
            buildFileTree();
            builtFileTree = true;
        }
        return children;
    }

    public void buildFileTree(){
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

        // TODO: 对目录文件进行排序，并指定页首页尾

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
        if(archive == null)
            archive = this.getArchiveImplement().createArchive(url);
        return archive;
    }

    @Override
    protected void finalize() throws Throwable{
        if(this.archive != null)
            this.archive.close();
    }
}
