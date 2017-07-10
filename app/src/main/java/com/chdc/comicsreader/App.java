package com.chdc.comicsreader;

import android.app.Application;
import android.content.Context;

/**
 * Created by Wen on 2017/7/9.
 */

public class App extends Application {

    public static App app = new App();//单例化Application
    public static App getApp() {
        if (app == null) {
            synchronized (App.class) { //线程安全
                if (app == null) {
                    app = new App();
                }
            }
        }
        return app;
    }

    //上下文
    public static Context context;
    public static Context getContext() {    return context;}

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

}
