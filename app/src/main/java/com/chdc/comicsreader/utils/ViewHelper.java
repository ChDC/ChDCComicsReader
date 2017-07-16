package com.chdc.comicsreader.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.preference.Preference;
import android.support.v4.provider.DocumentFile;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.chdc.comicsreader.App;
import com.chdc.comicsreader.R;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Wen on 2017/7/8.
 */

public class ViewHelper {

    public static final ViewHelper INSTANCE = new ViewHelper();

    private ViewHelper() {

    }


    private Uri documentUri = null;

//    public String[] sdcardPaths;
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
        loadDocumentURI();
    }


    public void loadDocumentURI(){
        SharedPreferences preferences = App.getContext().getSharedPreferences("config", Activity.MODE_PRIVATE);
        // 获取读取外置存储卡文件的权限
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            String uriString = preferences.getString("document_uri", null);
            if(uriString != null){
                documentUri = Uri.parse(uriString);
            }
        }
    }

    private static final int REQUEST_CODE_STORAGE_ACCESS = 2;
    public void requestDocumentPermission(Activity activity){
        Intent intent =  new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        activity.startActivityForResult(intent, REQUEST_CODE_STORAGE_ACCESS);
    }


    public void triggerOnActivityResultForGetDocumentPermission(Activity activity, final int requestCode, final int resultCode, final Intent resultData){
        if (requestCode == REQUEST_CODE_STORAGE_ACCESS) {
            if (resultCode == Activity.RESULT_OK) {
                Uri treeUri = resultData.getData();
                activity.getSharedPreferences("config", Activity.MODE_PRIVATE)
                        .edit()
                        .putString("document_uri", treeUri.toString())
                        .apply();
                documentUri = treeUri;
                // Persist access permissions.
                int takeFlags = resultData.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                activity.getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
            }
        }
    }

    public boolean includeByExternalSDCard(String url){
        String esd = this.getExternalSDCardPath();
        if(esd == null)
            return false;
        return url != null && url.startsWith(esd);
    }

    public DocumentFile getDocumentFile(File file, final boolean isDirectory) {
        if (documentUri == null)
            return null;
        Context context = App.getContext();
        String baseFolder = getExternalSDCardPath();
        if (baseFolder == null)
            return null;

        String relativePath;
        try {
            String fullPath = file.getCanonicalPath();
            relativePath = fullPath.substring(baseFolder.length() + 1);
        }
        catch (IOException e) {
            return null;
        }

        // start with root of SD card and then parse through document tree.
        DocumentFile document = DocumentFile.fromTreeUri(context, documentUri);
        String[] parts = relativePath.split("/");
        for (int i = 0; i < parts.length; i++) {
            DocumentFile nextDocument = document.findFile(parts[i]);

            if (nextDocument == null) {
                if ((i < parts.length - 1) || isDirectory)
                    nextDocument = document.createDirectory(parts[i]);
                else
                    nextDocument = document.createFile("image", parts[i]);
            }
            document = nextDocument;
        }
        return document != null && document.canWrite() ? document : null;
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

    public Bitmap decodeBitmapFromStream(InputStream is, BitmapFactory.Options bitmapInfo){

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

    private String externalSDCardPath;
    public String getExternalSDCardPath(){
        if(externalSDCardPath == null)
            externalSDCardPath = this.getStoragePath(true);
        return externalSDCardPath;
    }

    private String getStoragePath(boolean is_removale) {

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

//    public void toggleVisibility(View view){
//        if(view.getVisibility() == View.VISIBLE)
//            view.setVisibility(View.INVISIBLE);
//        else
//            view.setVisibility(View.VISIBLE);
//    }

    /**
     * 获取用于打开图片文件的 Intent
     * @param file
     * @return
     */
    public Intent getImageFileIntent(File file) {

        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri = Uri.fromFile(file);
        intent.setDataAndType(uri, "image/*");
        return intent;
    }


    public void quitFullScreen(Activity activity){
        Window window = activity.getWindow();
        final WindowManager.LayoutParams attrs = window.getAttributes();
        attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.setAttributes(attrs);
        window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }

    public void enterFullScreen(Activity activity){
        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,  WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

//    public File getTempFile(String prefix, String suffix) throws IOException {
//        return App.getContext().getCacheDir().createTempFile(prefix, suffix);
//    }

//    public String[] getSDCardPaths(){
//        if(sdcardPaths == null){
//            sdcardPaths = getAllExtSdCardPaths();
//        }
//        return sdcardPaths;
//    }

//    private String[] getAllExtSdCardPaths() {
//        Context context = App.getContext();
//        List<String> paths = new ArrayList<>();
//        File[] externalFilesDirs = context.getExternalFilesDirs("external");
//        File externalFilesDir = context.getExternalFilesDir("external");
//        for (File file : externalFilesDirs) {
//            if (file != null && !file.equals(externalFilesDir)) {
//                int index = file.getAbsolutePath().lastIndexOf("/Android/data");
//                if (index < 0) {
//                    Log.w("Error", "Unexpected external file dir: " + file.getAbsolutePath());
//                }
//                else {
//                    String path = file.getAbsolutePath().substring(0, index);
//                    try {
//                        path = new File(path).getCanonicalPath();
//                    }
//                    catch (IOException e) {
//                        // Keep non-canonical path.
//                    }
//                    paths.add(path);
//                }
//            }
//        }
//        return paths.toArray(new String[paths.size()]);
//    }
}
