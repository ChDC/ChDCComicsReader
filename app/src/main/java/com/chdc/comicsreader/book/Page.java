package com.chdc.comicsreader.book;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.chdc.comicsreader.utils.ViewHelper;

import java.io.InputStream;
import java.util.List;

/**
 * Created by Wen on 2017/7/6.
 */

public class Page extends File{

    public PageType getPageType() {
        if(pageType != PageType.Unknown)
            return pageType;
        File parent = getParent();
        if(parent == null)
            pageType = PageType.HeadEnd;
        List<File> childrenOfParent = parent.getChildren();
        if(childrenOfParent == null || childrenOfParent.size() <= 0)
            pageType = PageType.HeadEnd;
        else{
            int i = childrenOfParent.indexOf(this);
            if(i >= 0)
                pageType = ((Page)childrenOfParent.get(i)).getPageType();
        }
        return pageType;
    }

    public void setPageType(PageType pageType) {
        this.pageType = pageType;
    }

    public enum PageType{
        /**
         * 未知
         */
        Unknown,
        /**
         * 非端点
         */
        NotEnd,
        /**
         * 开始页面
         * 如果该 Page 是目录中的唯一一个 Page，那么它也是 HeadEnd 类型
         */
        HeadEnd,
        /**
         * 结束页面
         */
        TailEnd,
    }

    private Bitmap bitmap;
    private boolean cannotLoadBitmap = false;
    private PageType pageType = PageType.Unknown;

    public Page(String url){
        super(url);
        this.cacheParent = true;
    }

    protected InputStream getInputStream(){
        return getFileImplement().getInputStream(url);
    }

    protected synchronized void refreshBitmap(){
        if(bitmap != null && !bitmap.isRecycled())
            return;

        BitmapFactory.Options options;
        try (InputStream is = this.getInputStream()) {
            options = ViewHelper.INSTANCE.getBitmapOption(is);
        } catch (Exception e) {
            e.printStackTrace();
            bitmap = null;
            return;
        }
        try (InputStream is = this.getInputStream()) {
            bitmap = ViewHelper.INSTANCE.decodeStream(is, options);
            return;
        } catch (Exception e) {
            bitmap = null;
            e.printStackTrace();
            return;
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
            refreshBitmap();
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
        if(bitmap == null)
            this.recycleBitmap();
        this.bitmap = bitmap;
    }


    public void clear(){
        this.parent = null;
        cannotLoadBitmap = false;
        pageType = PageType.Unknown;
    }
}
