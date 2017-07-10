package com.chdc.comicsreader.book;

import com.chdc.comicsreader.ViewHelper;

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

    protected Object[] getSiblingPage(Page page, int direction){
        boolean isLoadFromAnotherChapter = false;
        Page returnPage = null;
        File file = page;
        while(file != null && !Objects.equals(file.getUrl(), this.url)){
            File sibling = file.getSibling(direction >= 0 ? 1 : -1 );
            if(sibling == null){
                isLoadFromAnotherChapter = true;
                file = file.getParent();
            }
            else {
                Page p = direction >= 0 ? sibling.getFirstPage() : sibling.getTheLastPage();
                if(p != null){
                    returnPage = p;
                    break;
                }
                else
                    file = sibling;
            }
        }
        return new Object[]{returnPage, isLoadFromAnotherChapter};
    }

    public Object[] getNextPage(Page page){
        return this.getSiblingPage(page, 1);
    }

    public Object[] getLastPage(Page page){
        return this.getSiblingPage(page, -1);
    }

    @Override
    public File getParent() {
        return null;
    }
}
