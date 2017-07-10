package com.chdc.comicsreader.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.chdc.comicsreader.R;
import com.chdc.comicsreader.ViewHelper;
import com.chdc.comicsreader.book.Book;
import com.chdc.comicsreader.book.Page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Wen on 2017/7/7.
 */

public class PagesViewAdapter extends RecyclerView.Adapter<PagesViewAdapter.PageHolder> {

    private static final String TAG = "PagesViewAdapter";

    Context context;
    private Book book;
    RecyclerView owner;
    private Page startPage;
    SparseArray<Page> pageCache;
    int cachePageNumber = 2; // 缓存两个图片
    boolean isStartPageLoaded = false;
    boolean isTheLastPageLoaded = false;
    boolean isFirstPageLoaded = false;

    // 元素数量
    private int itemCount = 0;

    public PagesViewAdapter(Context context, RecyclerView owner){
        this.context = context;
        this.owner = owner;
        pageCache = new SparseArray<>(6);
    }

    @Override
    public PageHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        PageHolder holder = new PageHolder(LayoutInflater.from(context).inflate(R.layout.list_item_page, parent, false));
        return holder;
    }

    public Direction getDirection(int position){

        int firstPosition = ((LinearLayoutManager)owner.getLayoutManager()).findFirstVisibleItemPosition();
        if(firstPosition < 0)
            return Direction.CURRENT;
        else if(position < firstPosition)
            return Direction.LAST;
        else  if(position > firstPosition)
            return Direction.NEXT;
        return null;

//        Direction direction;
//        PageHolder last = (PageHolder) this.owner.findViewHolderForLayoutPosition(position - 1);
//        PageHolder next = (PageHolder) this.owner.findViewHolderForLayoutPosition(position + 1);
//        if(last == null && next != null)
//            direction = Direction.LAST;
//        else if(last != null && next == null)
//            direction = Direction.NEXT;
//        else
//            direction = Direction.CURRENT;
//        return direction;
    }

    @Override
    public void onBindViewHolder(PageHolder holder, int position) {

        // 确定方向
        Direction direction = getDirection(position);

        Page page = pageCache.get(position);
        if(page == null){
            // 查找前一个和后一个 View
            switch (direction){
                case LAST:
                    PageHolder next = (PageHolder) this.owner.findViewHolderForLayoutPosition(position + 1);
                    page = (Page)book.getLastPage(next.getPage())[0];
                    if(page == null){
                        if(isFirstPageLoaded){
                            setBlankHolder(holder);
                            return;
                        }
                        isFirstPageLoaded = true;
                        int offset = - position - 1;
                        // 更新移动缓存中的键
                        new Handler().post(() -> {
                            try {
                                offsetAllKeysInCache(offset);
                                this.itemCount += offset;
//                                newViewPosition += offset;
                                this.notifyDataSetChanged();
                                this.owner.scrollToPosition(0);
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                        });
                        setBlankHolder(holder);
                        Log.d(TAG, "前面没有了");
                        return;
                    }
                    break;
                case NEXT:
                    PageHolder last = (PageHolder) this.owner.findViewHolderForLayoutPosition(position - 1);
                    page = (Page)book.getNextPage(last.getPage())[0];
                    if(page == null){
                        if(!isTheLastPageLoaded) {
                            isTheLastPageLoaded = true;
                            new Handler().post(() -> {
                                try {
                                    this.itemCount = position;
                                    this.notifyDataSetChanged();
                                } catch (Exception e1) {
                                    e1.printStackTrace();
                                }
                            });
                            Log.d(TAG, "后面没有了");
                        }
                        setBlankHolder(holder);
                        return;
                    }
                    break;
                case CURRENT:
                    if(isStartPageLoaded){
                        setBlankHolder(holder);
                        return;
                    }
                    page = startPage;
                    isStartPageLoaded = true;
                    break;
            }
        }

//        newViewPosition = position;
        Page np = page;
        holder.setPage(page);
        if(page.isLoadedBitmap())
            holder.setBitmap(page.getBitmap());
        else{
            holder.setBitmap(ViewHelper.INSTANCE.getLoadingPicture());
            new Thread(() -> {
                Bitmap bitmap = np.getBitmap();
                this.owner.post(() -> {
                    holder.setBitmap(bitmap);
                });
            }).start();
        }

        // 缓存 Pages
        new Thread(() -> {
            cachePages(np, position, direction, cachePageNumber);
        }).start();
    }

    void setBlankHolder(PageHolder holder){
        holder.setPage(null);
        holder.setBitmap(BitmapFactory.decodeResource(this.context.getResources(), R.drawable.blank_picture));
    }

    public void offsetAllKeysInCache(int offset) throws Exception {
        if(offset > 0)
            return;
        // 获取所有的 Key
        List<Integer> keys = new ArrayList<>();
        for(int i = 0; i < pageCache.size(); i++)
            keys.add(pageCache.keyAt(i));
        Collections.sort(keys);
        for(int i = 0; i < keys.size(); i++){
            int key = keys.get(i);
            int newKey = key + offset;
            if(newKey < 0)
                continue;
            // this.notifyItemMoved(key, newKey);
            pageCache.put(newKey, pageCache.get(key));
            pageCache.remove(key);
        }
    }

    /**
     * 缓存页面
     * @param page
     * @param position
     * @param direction
     * @param count
     */
    public void cachePages(Page page, int position, Direction direction, int count){
        if(page == null)
            return;
        if(direction == Direction.CURRENT)
            direction = Direction.NEXT;

        if(pageCache.get(position) == null)
            pageCache.put(position, page);
        for(int i = 0; i < count; i++){
            position += direction == Direction.NEXT ? 1 : -1;
            Page cp = pageCache.get(position);
            if(cp != null){
                page = cp;
            }
            else{
                Object[] result = direction == Direction.NEXT ? book.getNextPage(page) : book.getLastPage(page);
                page = (Page)result[0];
                if(page == null)
                    break;
                if(pageCache.get(position) == null)
                    pageCache.put(position, page);
            }
            page.getBitmap();
        }
    }

    public Page getCurrentPage(){
        LinearLayoutManager linearLayoutManager = ((LinearLayoutManager)this.owner.getLayoutManager());
        int position = linearLayoutManager.findFirstCompletelyVisibleItemPosition();
        if(position < 0)
            position = linearLayoutManager.findFirstVisibleItemPosition();
        if(position < 0)
            return startPage;
        return pageCache.get(position);
    }

    public boolean loadPage(Page page){
        // clear
        this.clear();
        startPage = page;

        this.setItemCount(Integer.MAX_VALUE);
        if(startPage == null || !startPage.isValid()) {
            startPage = book.getFirstPage();
        }
        else {
            this.owner.scrollToPosition(1000);
        }

        notifyDataSetChanged();
        if(startPage == null || !startPage.isValid())
            return false;
        return true;
    }

    public void clear(){
        clearCache();
        isStartPageLoaded = false;
        isTheLastPageLoaded = false;
        isFirstPageLoaded = false;
        setItemCount(0);
    }


    public void clearCache(){
        int size = pageCache.size();
        for(int i = 0; i < size; i++) {
            Page page = pageCache.valueAt(i);
            if(page != null)
                page.recycleBitmap();
        }
        pageCache.clear();
    }

    /**
     * 将在位置 position 以外的所有缓存清除（包括 position ）
     * @param position
     * @param direction
     */
    public void removeCache(int position, Direction direction){
        if(direction == Direction.CURRENT){
            Page page = pageCache.get(position);
            if(page != null)
                page.recycleBitmap();
            pageCache.remove(position);
            return;
        }

        int index = pageCache.indexOfKey(position);
        if(index < 0)
            return;

        List<Integer> keys = new ArrayList<>();
        keys.add(position);
        int size = pageCache.size();
        for(int i = 0; i < size; i++ ) {
            int key = pageCache.keyAt(i);
            switch (direction){
                case NEXT:
                    if(key > position)
                        keys.add(key);
                    break;
                case LAST:
                    if(key < position)
                        keys.add(key);
                    break;
            }
        }
        size = keys.size();
        for(int i = 0; i < size; i++) {
            Page page = pageCache.get(keys.get(i));
            if(page != null)
                page.recycleBitmap();
            pageCache.remove(keys.get(i));
        }
    }

    @Override
    public void onViewRecycled(PageHolder holder) {
        super.onViewRecycled(holder);

        int position = holder.getAdapterPosition();
        if(position <0 || position > itemCount)
            return;
        Direction direction = getDirection(position); // position > newViewPosition ? Direction.NEXT : Direction.LAST;

        // 清理缓存
        position  += cachePageNumber * (direction == Direction.NEXT ? 1 : -1);
        if(position <0 || position > itemCount)
            return;
        int np = position;
        new Thread(() -> {
            removeCache(np, direction);
        }).start();
    }

    @Override
    public int getItemCount() {
        return this.itemCount;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public Page getStartPage() {
        return startPage;
    }

    public void setStartPage(Page startPage) {
        this.startPage = startPage;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public class PageHolder extends RecyclerView.ViewHolder{

        ImageView imageView;
        private Page page;

        public PageHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.page);
        }

        public void setPage(Page page){
            this.page = page;
        }

        public void setBitmap(Bitmap bitmap){
            if(bitmap == null || bitmap.isRecycled()){
                // TODO: 添加错误图片
                imageView.setImageBitmap(ViewHelper.INSTANCE.getErrorPicture());
                return;
            }
            imageView.setImageBitmap(bitmap);
        }

        public Page getPage() {
            return page;
        }
    }
}
