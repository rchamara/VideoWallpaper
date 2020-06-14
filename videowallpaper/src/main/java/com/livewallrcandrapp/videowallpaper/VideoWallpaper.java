package com.livewallrcandrapp.videowallpaper;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

public class VideoWallpaper {

    private static final String TAG = "VideoWallpaper";

    private String mVideoUrl;
    private boolean isLooping = false;
    private Context mContext;
    private Uri mVideoUri;

    /**
     * set application context
     * @param context
     */
    public void applicationContext(Context context) {
        this.mContext = context;
    }

    /**
     * set video url as string
     * @param url
     */
    public void videoUrl(String url) {
        Log.i(TAG, "url: "+url);
        if (url.isEmpty()) {
            Log.e(TAG, "video url is empty enter valid url");
        }
        this.mVideoUrl = url;
    }

    /**
     * save user relate data in shared_preferences
     */
    private void saveDataInSharedPreferences() {
        try {
            SharedPreferences mSharedPreferences = mContext.getSharedPreferences(Utility.VIDEO_WALLPAPER_DATA, Context.MODE_PRIVATE);
            SharedPreferences.Editor mEditor = mSharedPreferences.edit();
            mEditor.putString(Utility.M_VIDEO_URL,mVideoUrl);
            mEditor.putBoolean(Utility.IS_LOOPING, isLooping);
            mEditor.commit();
            Log.i(TAG,"[saveDataInSharedPreferences] saved data");
        } catch (Exception exc) {
            Log.e(TAG,"Error in save data: " +exc.getMessage());
        }
    }

    /**
     * set looping state
     * @param isLooping
     */
    public void setIsLooping(boolean isLooping) {
        Log.i(TAG, "isLooping:" +isLooping);
       this.isLooping = isLooping;
    }

    /**
     * convert URL to Uri object
     * @return
     */
    private Uri mVideoUrlToUri() {
        Uri uri = null;
        try {
            uri = Uri.parse(mVideoUrl);
        } catch (Exception exc) {
            Log.e(TAG, "Can not convert URL: "+exc.getMessage());
        }
        return uri;
    }

    /**
     * set wallpaper using wallpaper services
     */
    public void Set() {
        mVideoUri = mVideoUrlToUri();
        if (mVideoUri != null) {

            try {
                WallpaperManager wallpaperManager = WallpaperManager.getInstance(mContext);
                wallpaperManager.clear();
            } catch (Exception exc) {
                Log.e(TAG, "[Set] can not clear wallpaper Exception error: "+exc.getMessage());
            }

            Intent mIntent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            mIntent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    new ComponentName(mContext, VideoWallpaperService.class));
            mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            saveDataInSharedPreferences();
            Log.i(TAG, "[isLooping:]"+isLooping);
            Log.i(TAG, "[video_uri:]"+mVideoUri);
            Log.i(TAG, "calling wallpaper service");
            mContext.startActivity(mIntent);
        } else {
            Log.e(TAG, "Video URl is not valid");
        }

    }


}
