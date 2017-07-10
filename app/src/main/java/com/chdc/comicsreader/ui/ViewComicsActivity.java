package com.chdc.comicsreader.ui;

import android.content.Intent;
import android.content.res.Configuration;
import android.nfc.Tag;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.chdc.comicsreader.R;
import com.chdc.comicsreader.ViewHelper;
import com.chdc.comicsreader.book.Book;
import com.chdc.comicsreader.book.Page;

import java.util.Objects;

public class ViewComicsActivity extends AppCompatActivity {

    private static final String TAG = "ViewComicsActivity";
    Book book;
    Page startPage;
    RecyclerView pagesView;
    PagesViewAdapter pagesViewAdapter;

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
        pagesViewAdapter = new PagesViewAdapter(this, pagesView);
        pagesView.setAdapter(pagesViewAdapter);
    }

    public void handleIntent(Intent intent){
        if(intent == null)
            return;
        String action = intent.getAction();

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
//            pagesViewAdapter.setStartPage(startPage);
            if(!pagesViewAdapter.loadPage(startPage)){
                Toast.makeText(this, "该漫画为空", Toast.LENGTH_SHORT).show();
                finish();
//            pagesViewAdapter.setItemCount(0);
                return;
            }
        }
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

}
