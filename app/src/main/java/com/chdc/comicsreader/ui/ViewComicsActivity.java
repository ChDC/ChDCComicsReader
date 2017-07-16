package com.chdc.comicsreader.ui;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.chdc.comicsreader.R;
import com.chdc.comicsreader.archive.ArchiveFile;
import com.chdc.comicsreader.archive.ArchivePage;
import com.chdc.comicsreader.fs.ArchiveBridgeFile;
import com.chdc.comicsreader.fs.File;
import com.chdc.comicsreader.utils.RecyclerItemClickListener;
import com.chdc.comicsreader.utils.ViewHelper;
import com.chdc.comicsreader.book.Book;
import com.chdc.comicsreader.book.Page;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ViewComicsActivity extends AppCompatActivity {

    Handler handler = new Handler();
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
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
        ViewHelper.INSTANCE.enterFullScreen(this);

        setContentView(R.layout.activity_view_comics);

        ViewHelper.INSTANCE.init(this);
        loadViews();
        changeForScreenRotated(getResources().getConfiguration());

        handleIntent(getIntent());
    }

    public void loadViews(){
        pagesView = (RecyclerView) findViewById(R.id.pagesView);
        LinearLayoutManager pageLayoutManager = new LinearLayoutManager(this);

        pagesView.setLayoutManager(pageLayoutManager);
        pagesViewAdapter = new PagesViewAdapter(this, pagesView, pool);
        pagesView.setAdapter(pagesViewAdapter);
        toolbar = findViewById(R.id.toolbar);

        ArchiveBridgeFile.setPasswordIsWrongListener(abf -> handler.post(() ->{
            // 密码错误
            // 重新输入密码，然后重新加载
            final EditText input = new EditText(this);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.msg_input_password);
            builder.setView(input);
            builder.setNegativeButton(R.string.title_cancel, null);
            builder.setPositiveButton(R.string.title_ok, (d,i) -> {
                String password = input.getText().toString().trim();
                abf.setPassword(password);
                pagesViewAdapter.loadPage(abf.getHeadEndPage());
            });
                builder.create().show();
        }));

        findViewById(R.id.btnRotateScreen).setOnClickListener(v -> {
            switch (getResources().getConfiguration().orientation){
                case Configuration.ORIENTATION_LANDSCAPE:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                    break;

                case Configuration.ORIENTATION_PORTRAIT:
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                    break;

                default:
                    Log.e(TAG, "undefined value");
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                    break;
            }
            hideToolbar();
        });

        pagesView.setOnClickListener(v -> {
            toolbar.setVisibility(View.VISIBLE);
        });

        pagesView.addOnItemTouchListener(
                new RecyclerItemClickListener(this, pagesView, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override public void onItemClick(View view, int position) {
                        // do whatever
                        if(toolbar.getVisibility() == View.VISIBLE)
                            hideToolbar();
                        else
                            showToolbar();
                    }

                    @Override public void onLongItemClick(View view, int position) {
                        // do whatever
                        try {
                            PagesViewAdapter.PageHolder vh = (PagesViewAdapter.PageHolder) pagesView.findViewHolderForAdapterPosition(position);
                            Page cp = vh.getPage();
                            Intent intent = ViewHelper.INSTANCE.getImageFileIntent(cp.getCachedFile());
                            startActivity(intent);
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                })
        );

        findViewById(R.id.btnNextChapter).setOnClickListener(v -> {
            Page cp = pagesViewAdapter.getCurrentPage();
            Page np = book.getNextChapterPage(cp);
            if(np == null)
                ViewHelper.INSTANCE.showMessage(getString(R.string.msg_lastchapter));
            else
                pagesViewAdapter.loadPage(np);
            hideToolbar();
        });

        findViewById(R.id.btnLastChapter).setOnClickListener(v -> {
            Page cp = pagesViewAdapter.getCurrentPage();
            if(cp.getPageType() == Page.PageType.HeadEnd){
                Page np = book.getLastChapterPage(cp);
                if(np == null)
                    ViewHelper.INSTANCE.showMessage(getString(R.string.msg_firstchapter));
                else
                    pagesViewAdapter.loadPage(np);
            }
            else {
                Page np = (Page)cp.getParent().getChildren().get(0);
                pagesViewAdapter.loadPage(np);
            }
            hideToolbar();
        });

        findViewById(R.id.btnDelete).setOnClickListener(v -> {
            File cp = pagesViewAdapter.getCurrentPage();
            if(cp == null || cp.getParent() == null){
                ViewHelper.INSTANCE.showMessage(getString(R.string.msg_emptychapter));
                return;
            }
            // 如果是压缩文件
            String title;
            if(cp instanceof ArchivePage) {
                cp = ((ArchivePage) cp).getArchiveBridgeFile();
                title = cp.getURL();
            }
            else {
                title = cp.getParent().getURL();
            }
            final File currentFile = cp;

            // 创建 AlertDialog.Builder 对象
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.title_deletechapter));
            builder.setMessage(String.format(getString(R.string.msg_affirm_delete_chapter), title));
            builder.setPositiveButton(R.string.title_ok, (d, i) -> {
                try{
                    deleteChapter(currentFile);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            });
            builder.setOnDismissListener(d -> hideToolbar());
            builder.setNegativeButton(R.string.title_cancel, null);
            AlertDialog ad = builder.create();
            ad.show();
        });
    }

    public void deleteChapter(File chapter){

        Page np = book.getNextChapterPage(chapter);
        if(np == null)
            np = book.getLastChapterPage(chapter);

        // 删除本章
        boolean success = chapter instanceof Page ? chapter.getParent().delete() : chapter.delete();
        if(!success){
            ViewHelper.INSTANCE.showMessage(getString(R.string.msg_fail_to_delete));
            // 检查是否是外部存储卡，如果是就请求权限
            if(ViewHelper.INSTANCE.includeByExternalSDCard(chapter.getURL()))
                ViewHelper.INSTANCE.requestDocumentPermission(this);
            return;
        }
        if(np == null){
            finishForEmpty();
            return;
        }
        // 加载下一章
        pagesViewAdapter.loadPage(np);
    }

    public void handleIntent(Intent intent){
        if(intent == null)
            return;
        String action = intent.getAction();

        Page startPage;
        if(Intent.ACTION_VIEW.equals(action)){
            // 从文件中打开
            String file =  ViewHelper.INSTANCE.getPath(intent.getData());
            book = Book.getBookByImageURL(file);
            startPage = File.getFirstPageFromFile(file);
        }
        else{
            // 从其他 Activivy 中打开
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
        ViewHelper.INSTANCE.showMessage(getString(R.string.msg_emptybook));
        finish();
    }


    public void hideToolbar(){
        ViewHelper.INSTANCE.hideView(toolbar);
        ViewHelper.INSTANCE.enterFullScreen(this);
    }

    public void showToolbar(){
        ViewHelper.INSTANCE.showView(toolbar);
        ViewHelper.INSTANCE.quitFullScreen(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    public void changeForScreenRotated(Configuration newConfig){
        switch (newConfig.orientation){
            case Configuration.ORIENTATION_LANDSCAPE:
                ViewHelper.INSTANCE.setLandScape(true);
                break;

            case Configuration.ORIENTATION_PORTRAIT:
                ViewHelper.INSTANCE.setLandScape(false);
                break;

            default:
                Log.e(TAG, "undefined value");
                ViewHelper.INSTANCE.setLandScape(true);
                break;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        changeForScreenRotated(newConfig);
//        ViewHelper.INSTANCE.init(this);
        // 重新加载图片
        pagesViewAdapter.loadPage(pagesViewAdapter.getCurrentPage());
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        ViewHelper.INSTANCE.triggerOnActivityResultForGetDocumentPermission(this, requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        pool.shutdownNow();
    }
}
