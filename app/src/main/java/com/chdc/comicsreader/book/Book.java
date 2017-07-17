package com.chdc.comicsreader.book;

import com.chdc.comicsreader.archive.ArchivePage;
import com.chdc.comicsreader.fs.ArchiveBridgeFile;
import com.chdc.comicsreader.fs.File;
import com.chdc.comicsreader.fs.FileImplement;

import java.util.List;
import java.util.Objects;

/**
 * Created by Wen on 2017/7/6.
 */

public class Book extends File {


    public static Book getBookByImageURL(String url){
        if(url == null)
            return null;
        FileImplement fi = FileImplement.getFileImplementByURLType(url);
        if(fi == null)
            return null;
        return new Book(fi.getRootPath(url));
    }

    public Book(String rootPath){
        super(rootPath);
    }

    /**
     * 获取下一个文件下的第一页
     * @param file
     * @param direction
     * @return
     */
    protected Page getSiblingPage(File file, int direction){
        while(file != null && !Objects.equals(file.getURL(), this.url)){
            File sibling = file.getSibling(direction >= 0 ? 1 : -1 );
            if(sibling == null){
                file = file.getParent();
            }
            else {
                Page page = direction >= 0 ? sibling.getHeadEndPage() : sibling.getTailEndPage();
                if(page != null)
                    return page;
                else
                    file = sibling;
            }
        }
        return null;
    }

    public Page getNextPage(Page page){
        return this.getSiblingPage(page, 1);
    }

    public Page getLastPage(Page page){
        return this.getSiblingPage(page, -1);
    }

    public Page getNextChapterPage(File page){
        if(page.isPage()) {
            // 考虑目录中既有文件又有目录的情况
            List<File> childrenOfParent = page.getParent().getChildren();
            int i;
            for(i = childrenOfParent.size() - 1; i >= 0 && childrenOfParent.get(i).isDirectory(); i--) {

            }
            return this.getSiblingPage(childrenOfParent.get(i), 1);
        }
        else{
            // 目录
            return this.getSiblingPage(page, 1);
        }
    }

    public Page getTailPageOfLastChapter(File page){
        File file = page.isPage() ? page.getParent() : page;
        return this.getSiblingPage(file, -1);
    }

    public Page getLastChapterPage(File page){
        File file = page.isPage() ? page.getParent() : page;
        Page p = this.getSiblingPage(file, -1);
        return (Page)p.getParent().getChildren().get(0);
    }

    @Override
    public File getParent() {
        return null;
    }

    public boolean deleteChapter(File file){
        if(file instanceof ArchiveBridgeFile)
            return file.delete();
        else if(file instanceof ArchivePage){
            return ((ArchivePage)file).getArchiveBridgeFile().delete();
        }
        else if(file.isPage()){
            return file.getParent().delete();
        }
        return false;
    }

}
