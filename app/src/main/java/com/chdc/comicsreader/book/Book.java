package com.chdc.comicsreader.book;

import com.chdc.comicsreader.utils.ViewHelper;

import java.util.List;
import java.util.Objects;

/**
 * Created by Wen on 2017/7/6.
 */

public class Book extends File{

    protected static Book[] sdcardBooks;

    public static Book getBookByImageURL(String url){
        if(sdcardBooks == null){
            String isd = ViewHelper.INSTANCE.getInnerSDCardPath();
            String esd = ViewHelper.INSTANCE.getStoragePath(true);
            sdcardBooks = new Book[2];
            if(isd != null)
                sdcardBooks[0] = new Book(isd);
            if(esd != null)
                sdcardBooks[1] = new Book(esd);
        }
        if(url == null)
            return sdcardBooks[0];

        for(int i = 0; i < sdcardBooks.length; i++){
            if(url.startsWith(sdcardBooks[i].getUrl()))
                return sdcardBooks[i];
        }
        return new Book("/sdcard");
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
    protected Page getSiblingPage(File file, int direction, boolean willGetHeadEndPage){
        while(file != null && !Objects.equals(file.getUrl(), this.url)){
            File sibling = file.getSibling(direction >= 0 ? 1 : -1 );
            if(sibling == null){
                file = file.getParent();
            }
            else {
                Page page = willGetHeadEndPage ? sibling.getHeadEndPage() : sibling.getTailEndPage();
                if(page != null)
                    return page;
                else
                    file = sibling;
            }
        }
        return null;
    }

    public Page getNextPage(Page page){
        return this.getSiblingPage(page, 1, true);
    }

    public Page getLastPage(Page page){
        return this.getSiblingPage(page, -1, false);
    }

    public Page getNextChapterPage(Page page){
        // 考虑目录中既有文件又有目录的情况
        List<File> childrenOfParent = page.getParent().getChildren();
        int i = -1;
        for(File file : childrenOfParent){
            if(!(file instanceof Page))
                break;
            i++;
        }
        return this.getSiblingPage(childrenOfParent.get(i), 1, true);
    }

    public Page getLastChapterPage(Page page){
        // 考虑目录中既有文件又有目录的情况
        Page p = this.getSiblingPage(page.getParent(), -1, true);
        if(p == null)
            return null;
        // 如果有后继表明不是最后一个
        Page fp = (Page)p.getParent().getChildren().get(0);
        if(!fp.equals(p) && fp.isValid())
            return fp;
        else
            return p;
    }

    @Override
    public File getParent() {
        return null;
    }
}
