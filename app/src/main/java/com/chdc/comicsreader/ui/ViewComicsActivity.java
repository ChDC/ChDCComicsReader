package com.chdc.comicsreader.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.chdc.comicsreader.R;
import com.chdc.comicsreader.book.File;
import com.chdc.comicsreader.utils.RecyclerItemClickListener;
import com.chdc.comicsreader.utils.ViewHelper;
import com.chdc.comicsreader.book.Book;
import com.chdc.comicsreader.book.Page;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ViewComicsActivity extends AppCompatActivity {

    private static final String TAG = "ViewComicsActivity";
    RecyclerView pagesView;
    PagesViewAdapter pagesViewAdapter;
    LinearLayoutManager layoutManager;
    ExecutorService pool = Executors.newFixedThreadPool(4);
    View toolbar;
    Book book;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,  WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_view_comics);

        ViewHelper.INSTANCE.init(this);

        loadViews();

        handleIntent(getIntent());
    }

    public void loadViews(){
        pagesView = (RecyclerView) findViewById(R.id.pagesView);
        LinearLayoutManager pageLayoutManager = new LinearLayoutManager(this);
//        pageLayoutManager.setItemPrefetchEnabled(true);
//        pageLayoutManager.setInitialPrefetchItemCount(10);

        pagesView.setLayoutManager(pageLayoutManager);
        pagesViewAdapter = new PagesViewAdapter(this, pagesView, pool);
        pagesView.setAdapter(pagesViewAdapter);
        toolbar = findViewById(R.id.toolbar);

//        toolbar.setOnClickListener(v -> {
//            toolbar.setVisibility(View.INVISIBLE);
//        });

        pagesView.setOnClickListener(v -> {
            toolbar.setVisibility(View.VISIBLE);
        });

        pagesView.addOnItemTouchListener(
                new RecyclerItemClickListener(this, pagesView, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override public void onItemClick(View view, int position) {
                        // do whatever
                        ViewHelper.INSTANCE.toggleVisibility(toolbar);
                    }

                    @Override public void onLongItemClick(View view, int position) {
                        // do whatever
                    }
                })
        );

        findViewById(R.id.btnNextChapter).setOnClickListener(v -> {
            Page cp = pagesViewAdapter.getCurrentPage();
            Page np = book.getNextChapterPage(cp);
            if(np == null)
                ViewHelper.INSTANCE.showMessage("这已经是最后一章了！");
            else
                pagesViewAdapter.loadPage(np);
            ViewHelper.INSTANCE.hideView(toolbar);
        });

        findViewById(R.id.btnLastChapter).setOnClickListener(v -> {
            Page cp = pagesViewAdapter.getCurrentPage();
            if(cp.getPageType() == Page.PageType.HeadEnd){
                Page np = book.getLastChapterPage(cp);
                if(np == null)
                    ViewHelper.INSTANCE.showMessage("这已经是第一章了！");
                else
                    pagesViewAdapter.loadPage(np);
            }
            else {
                Page np = (Page)cp.getParent().getChildren().get(0);
                pagesViewAdapter.loadPage(np);
            }
            ViewHelper.INSTANCE.hideView(toolbar);
        });

        findViewById(R.id.btnDelete).setOnClickListener(v -> {
            // 创建 AlertDialog.Builder 对象
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("删除章节");
            builder.setMessage("确认删除章节？");
            builder.setPositiveButton("确定", (d, i) -> {
                try{
                    Page cp = pagesViewAdapter.getCurrentPage();
                    Page np = book.getNextChapterPage(cp);
                    if(np == null)
                        np = book.getLastPage(cp);
                    if(np == null){
                        finishForEmpty();
                        return;
                    }
                    // 删除本章
                    cp.delete();
                    // 加载下一章
                    pagesViewAdapter.loadPage(np);
                    ViewHelper.INSTANCE.hideView(toolbar);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            });
            AlertDialog ad = builder.create();
            ad.show();

        });
    }

    public void handleIntent(Intent intent){
        if(intent == null)
            return;
        String action = intent.getAction();

        Page startPage;
        if(Intent.ACTION_VIEW.equals(action)){
            String file =  ViewHelper.INSTANCE.getPath(intent.getData());
            book = Book.getBookByImageURL(file);
            startPage = new Page(file);
        }
        else{
            Bundle bundle = intent.getExtras();
            book = (Book)bundle.getSerializable("book");
            startPage = (Page)bundle.getSerializable("startPage");
        }



        if(pagesViewAdapter != null) {
            pagesViewAdapter.setBook(book);
            if(!pagesViewAdapter.loadPage(startPage)){
                finishForEmpty();
            }
        }
    }

    public void finishForEmpty(){
        ViewHelper.INSTANCE.showMessage("该书为空！");
        finish();
    }



    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        switch (newConfig.orientation){
            case Configuration.ORIENTATION_LANDSCAPE:
                ViewHelper.INSTANCE.setLandScape(true);
                break;

            case Configuration.ORIENTATION_PORTRAIT:
                ViewHelper.INSTANCE.setLandScape(false);
                break;

            default:
                Log.e(TAG, "undefined value");
                break;
        }
        // 重新加载图片
        ViewHelper.INSTANCE.init(this);
        pagesViewAdapter.loadPage(pagesViewAdapter.getCurrentPage());
    }


    @Override
    public void onDestroy(){
        super.onDestroy();
        pool.shutdownNow();
    }
}
