package com.chdc.comicsreader.book;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.chdc.comicsreader.ViewHelper;

import java.io.InputStream;
import java.util.List;

/**
 * Created by Wen on 2017/7/6.
 */

public class Page extends File{


    private Bitmap bitmap;
    private boolean cannotLoadBitmap = false;
    private boolean isFirstPage = false;
    private boolean isTheLastPage = false;

    public Page(String url){
        super(url);
        this.cacheParent = true;
    }

    protected InputStream getInputStream(){
        return getFileImplement().getInputStream(url);
    }

    public Bitmap refreshBitmap(){
        BitmapFactory.Options options;
        try (InputStream is = this.getInputStream()) {
            options = ViewHelper.INSTANCE.getBitmapOption(is);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        try (InputStream is = this.getInputStream()) {
            return ViewHelper.INSTANCE.decodeStream(is, options);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public File getParent(){
        File p = super.getParent();
        if(p == null)
            return null;
        p.cacheChildren = true;
        return p;
    }

    @Override
    public List<com.chdc.comicsreader.book.File> getChildren() {
        return null;
    }

    public boolean isLoadedBitmap(){
        return bitmap != null && !bitmap.isRecycled();
    }

    public Bitmap getBitmap() {
        if(!cannotLoadBitmap && (bitmap == null || bitmap.isRecycled())) {
            bitmap = refreshBitmap();
            if(bitmap == null)
                cannotLoadBitmap = true;
        }
        return bitmap;
    }

    /**
     * 回收图片资源
     */
    public void recycleBitmap(){
        if (bitmap != null && !bitmap.isRecycled())
            bitmap.recycle();
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public boolean isFirstPage() {
        return isFirstPage;
    }

    public void setFirstPage(boolean firstPage) {
        isFirstPage = firstPage;
    }

    public boolean isTheLastPage() {
        return isTheLastPage;
    }

    public void setTheLastPage(boolean theLastPage) {
        isTheLastPage = theLastPage;
    }
}
