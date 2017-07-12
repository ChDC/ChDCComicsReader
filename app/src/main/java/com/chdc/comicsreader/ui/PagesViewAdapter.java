package com.chdc.comicsreader.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.chdc.comicsreader.R;
import com.chdc.comicsreader.utils.ViewHelper;
import com.chdc.comicsreader.book.Book;
import com.chdc.comicsreader.book.Page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Wen on 2017/7/7.
 */

public class PagesViewAdapter extends RecyclerView.Adapter<PagesViewAdapter.PageHolder> {

    private static final String TAG = "PagesViewAdapter";

    private Context context;
    private RecyclerView owner;
    private ExecutorService pool;

    private Book book;
    private Page startPage;

    private SparseArray<Page> pageCache;
    private int cachePageNumber = 2; // 缓存两个图片
    private BlockingQueue<Page> loadBitmapQueue = new LinkedBlockingQueue<>(20);

    private boolean isStartPageLoaded = false;
    private boolean isLockedForChangeBackEnd = false;
    private boolean isLockedForChangeFrontEnd = false;

    private Handler handler = new Handler();

    // 元素数量
    private int itemCount = 0;

    public PagesViewAdapter(Context context, RecyclerView owner, ExecutorService pool){
        this.context = context;
        this.owner = owner;
        pageCache = new SparseArray<>(6);
        this.pool = pool;

        Thread loadBitmapThread = new Thread(() -> {
            while(true){
                try {
                    Page page = loadBitmapQueue.take();
                    if(page != null && pageCache.indexOfValue(page) >= 0)
                        page.getBitmap();
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
        loadBitmapThread.setDaemon(true);
        loadBitmapThread.start();
    }

    @Override
    public PageHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        PageHolder holder = new PageHolder(LayoutInflater.from(context).inflate(R.layout.list_item_page, parent, false));
        return holder;
    }

    private Direction getDirection(int position){

        int firstPosition = ((LinearLayoutManager)owner.getLayoutManager()).findFirstVisibleItemPosition();
        if(firstPosition < 0 || firstPosition == position)
            return Direction.CURRENT;
        else if(position < firstPosition)
            return Direction.LAST;
        else  if(position > firstPosition)
            return Direction.NEXT;
        return null;
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
                    if(next != null)
                        page = book.getLastPage(next.getPage());
                    break;
                case NEXT:
                    PageHolder last = (PageHolder) this.owner.findViewHolderForLayoutPosition(position - 1);
                    if(last != null)
                        page = book.getNextPage(last.getPage());
                    break;
                case CURRENT:
                    if(isStartPageLoaded){
                        setBlankHolder(holder);
                        return;
                    }
                    page = startPage;
                    isStartPageLoaded = true;
                    // 缓存 Pages
                    Page np = page;
                    pool.submit(() -> cachePages(np, position, Direction.LAST, cachePageNumber));
                    break;
            }
        }
        if(page == null){
            setBlankHolder(holder);
            return;
        }

        Page np = page;
        holder.setPage(page);
        if(page.isLoadedBitmap())
            holder.setBitmap(page.getBitmap());
        else{
            holder.setBitmap(ViewHelper.INSTANCE.getLoadingPicture());
            pool.submit(() -> {
                Bitmap bitmap = np.getBitmap();
                this.owner.post(() -> {
                    holder.setBitmap(bitmap);
                });
            });
        }

        // 缓存 Pages
        pool.submit(() -> cachePages(np, position, direction, cachePageNumber));
    }

    private void setBlankHolder(PageHolder holder){
        holder.setPage(null);
        holder.setBitmap(ViewHelper.INSTANCE.getBlankPicture());
    }

    private void offsetAllKeysInCache(int offset) throws Exception {
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
    private void cachePages(Page page, int position, Direction direction, int count){
        if(page == null)
            return;
        if(direction == Direction.CURRENT)
            direction = Direction.NEXT;

        pageCache.put(position, page);
        for(int i = 0; i < count; i++){
            position += direction == Direction.NEXT ? 1 : -1;
            if(position < 0 || position >= itemCount)
                break;
            Page cp = pageCache.get(position);
            if(cp != null)
                page = cp;
            else{
                page = direction == Direction.NEXT ? book.getNextPage(page) : book.getLastPage(page);
                if(page != null)
                    pageCache.put(position, page);
                else { //(page == null) {
                    // 到头了
                    if(direction == Direction.LAST && !isLockedForChangeFrontEnd) {
                        isLockedForChangeFrontEnd = true;
                        int offset = - position - 1;
                        handler.post(() -> {
                            try {
                                offsetAllKeysInCache(offset);
                                this.itemCount += offset;
                                this.notifyDataSetChanged();
                                this.owner.scrollToPosition(0);
                                isLockedForChangeFrontEnd = false;
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                        });
                    }
                    else if(direction == Direction.NEXT && !isLockedForChangeBackEnd) {
                        isLockedForChangeBackEnd = true;
                        int p = position;
                        handler.post(() -> {
                            try {
                                this.itemCount = p;
                                this.notifyDataSetChanged();
                                isLockedForChangeBackEnd = false;
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                        });
                    }
                    break;
                }
            }
            try {
                loadBitmapQueue.put(page);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
        page.clear();
        startPage = page;

        this.setItemCount(Integer.MAX_VALUE);
        if(startPage == null || !startPage.isValid()) {
            startPage = book.getHeadEndPage();
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
        isLockedForChangeBackEnd = false;
        isLockedForChangeFrontEnd = false;
        loadBitmapQueue.clear();
        setItemCount(0);
    }


    private void clearCache(){
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
    private void removeCache(int position, Direction direction){
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
        pool.submit(() -> {
            removeCache(np, direction);
        });
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

        TextView textView;
        ImageView imageView;
        private Page page;

        public PageHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.page);
            textView = (TextView) itemView.findViewById(R.id.title);
        }

        public void setPage(Page page){
            this.page = page;
            if(page != null && page.getPageType() == Page.PageType.HeadEnd)
                showTitle();
            else
                hideTitle();
        }

        public void setBitmap(Bitmap bitmap){
            if(bitmap == null || bitmap.isRecycled()){
                imageView.setImageBitmap(ViewHelper.INSTANCE.getErrorPicture());
                return;
            }
            imageView.setImageBitmap(bitmap);
        }

        public void showTitle(){
            try {
                String title = page.getParent().getUrl().replace(book.getUrl(), "");
                textView.setText(title);
                if(textView.getVisibility() == View.GONE) {
                    textView.setVisibility(View.VISIBLE);
                }
            }
            catch (Exception e){
                return;
            }
        }

        public void hideTitle(){
            if(textView.getVisibility() == View.VISIBLE)
                textView.setVisibility(View.GONE);
        }

        public Page getPage() {
            return page;
        }
    }
}
