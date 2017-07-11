package com.chdc.comicsreader.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Toast;

import com.chdc.comicsreader.App;
import com.chdc.comicsreader.R;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by Wen on 2017/7/8.
 */

public class ViewHelper {

    public static final ViewHelper INSTANCE = new ViewHelper();

    private ViewHelper() {}

    private int screenWidth;
    private int screenHeight;
    private int targetDensity;
    private boolean isLandScape = false;

    public void init(Activity activity){
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        targetDensity = activity.getResources().getDisplayMetrics().densityDpi;

        switch (activity.getResources().getConfiguration().orientation){
            case Configuration.ORIENTATION_LANDSCAPE:
                ViewHelper.INSTANCE.setLandScape(true);
                break;

            case Configuration.ORIENTATION_PORTRAIT:
                ViewHelper.INSTANCE.setLandScape(false);
                break;

            default:
                break;
        }
    }

//    public void releaseImageViewResource(ImageView imageView) {
//        if (imageView == null) return;
//        Drawable drawable = imageView.getDrawable();
//        if (drawable != null && drawable instanceof BitmapDrawable) {
//            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
//            Bitmap bitmap = bitmapDrawable.getBitmap();
//            if (bitmap != null && !bitmap.isRecycled()) {
//                bitmap.recycle();
//            }
//        }
//    }

    public BitmapFactory.Options getBitmapOption(InputStream is){
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);
            is.close();
            return options;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Bitmap decodeStream(InputStream is, BitmapFactory.Options bitmapInfo){

        BitmapFactory.Options options;
        if(bitmapInfo == null){
            options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inSampleSize = 2;
        }
        else if(screenHeight > 0){
            options = bitmapInfo;
            int x,y;
            if(isLandScape){
                int max = Math.max(screenWidth, screenHeight);
                int min = Math.min(screenHeight, screenWidth);
                x = max;
                y = max * max / min;
            }
            else{
                x = screenWidth;
                y = screenHeight;
            }
//            options.inSampleSize = calculateInSampleSize(options, x, y);
            // 刚刚大于屏幕宽度
            // options.inSampleSize = options.outWidth < x ? 1 : (int)Math.pow(2, Math.floor(Math.log(options.outWidth / x) / Math.log(2)));
            // 刚刚大于屏幕宽度的二倍
            options.inSampleSize = options.outWidth < x * 2 ? 1 : (int)Math.pow(2, Math.floor(Math.log(options.outWidth / x / 2) / Math.log(2)));
            double xSScale = ((double)options.outWidth) / ((double)x);
            double ySScale = ((double)options.outHeight) / ((double)y);
            double startScale = xSScale > ySScale ? xSScale : ySScale;
            options.inScaled = true;
            options.inDensity = (int) (targetDensity*startScale);
            options.inTargetDensity = targetDensity;
            options.inJustDecodeBounds = false;
        }
        else
            options = null;
        return BitmapFactory.decodeStream(is, null, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options,
                                     int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and
            // keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }



    /**
     * 从 URI 中获取文件路径
     * @param uri
     * @return
     */
    public String getPath(Uri uri) {

        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_data" };
            Cursor cursor = null;

            try {
                cursor = App.getContext().getContentResolver().query(uri, projection,null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it
            }
        }

        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public String getInnerSDCardPath(){
        return Environment.getExternalStorageDirectory().getPath();
    }

    public String getStoragePath(boolean is_removale) {

        Context mContext = App.getContext();
        StorageManager mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        Class<?> storageVolumeClazz = null;
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
            Object result = getVolumeList.invoke(mStorageManager);
            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String path = (String) getPath.invoke(storageVolumeElement);
                boolean removable = (Boolean) isRemovable.invoke(storageVolumeElement);
                if (is_removale == removable) {
                    return path;
                }
            }
        } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Bitmap blankPicture;
    public Bitmap getBlankPicture() {
        if(blankPicture == null || blankPicture.isRecycled())
            blankPicture = BitmapFactory.decodeResource(App.getContext().getResources(), R.drawable.blank_picture);
        return blankPicture;
    }

    private Bitmap errorPicture;
    public Bitmap getErrorPicture(){
        if(errorPicture == null || errorPicture.isRecycled())
            errorPicture = BitmapFactory.decodeResource(App.getContext().getResources(), R.drawable.error_image);
        return errorPicture;
    }

    private Bitmap loadingPicture;
    public Bitmap getLoadingPicture(){
        if(loadingPicture == null || loadingPicture.isRecycled())
            loadingPicture = BitmapFactory.decodeResource(App.getContext().getResources(), R.drawable.loading_picture);
        return loadingPicture;
    }

    public void setBlankPicture(Bitmap blankPicture) {
        this.blankPicture = blankPicture;
    }

    public boolean isLandScape() {
        return isLandScape;
    }

    public void setLandScape(boolean landScape) {
        isLandScape = landScape;
    }

    public void showMessage(String message){
        Toast.makeText(App.getContext(), message, Toast.LENGTH_SHORT).show();
    }

    public void showView(View view){
        view.setVisibility(View.VISIBLE);
    }

    public void hideView(View view){
        view.setVisibility(View.INVISIBLE);
    }

    public void toggleVisibility(View view){
        if(view.getVisibility() == View.VISIBLE)
            view.setVisibility(View.INVISIBLE);
        else
            view.setVisibility(View.VISIBLE);
    }

}
