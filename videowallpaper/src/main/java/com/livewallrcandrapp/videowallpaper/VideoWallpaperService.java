package com.livewallrcandrapp.videowallpaper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;

public class VideoWallpaperService extends WallpaperService {

    private static final String TAG = "VideoWallpaperService";
    private MediaPlayer mMediaPlayer;
    private BroadcastReceiver mBroadcastReceiver = null;
    private Uri mVideoUri;
    private String mVideoURL;
    private boolean isLooping;

    /**
     * call when engine is created
     * @return
     */
    @Override
    public Engine onCreateEngine() {
        mMediaPlayer = new MediaPlayer();
        registerBroadcastReceiver();
        return new VideoEngine();
    }

    /**
     * register the screen on off receiver
     */
    private void registerBroadcastReceiver() {
        IntentFilter mIntentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        if ( mBroadcastReceiver == null ) {
            mBroadcastReceiver = new ScreenOnOffReceiver();
            registerReceiver(mBroadcastReceiver, mIntentFilter);
        }
    }

    /**
     * get screen  ON/OFF state
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean screenOn = false;
        try {
            screenOn = intent.getBooleanExtra("screen_state", false);
        } catch (Exception exc) {

        }

        if (!screenOn) {
            if (mMediaPlayer != null) {
                mMediaPlayer.start();
            }
        } else {
            if (mMediaPlayer != null) {
                mMediaPlayer.stop();
            }
        }
        return START_NOT_STICKY;
    }

    /**
     * call when services is destroyed
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
        }
    }

    /**
     * get user related data from SharedPreferences
     */
    private void getSharedPreferencesData() {
        try {
            SharedPreferences mSharedPreferences = getApplicationContext().getSharedPreferences(Utility.VIDEO_WALLPAPER_DATA, Context.MODE_PRIVATE);
            mVideoURL = mSharedPreferences.getString(Utility.M_VIDEO_URL, null);
            isLooping = mSharedPreferences.getBoolean(Utility.IS_LOOPING, false);
            Log.i(TAG,"mVideoURL : " +mVideoURL);
            Log.i(TAG, "isLooping : " +isLooping);
        } catch (Exception exc) {
            Log.e(TAG, "error in getSharedPreferences: " +exc.getMessage());
        }

    }

    /**
     * convert URL to Uri object
     * @return
     */
    private Uri mVideoUrlToUri() {
        Uri uri = null;
        try {
            uri = Uri.parse(mVideoURL);
        } catch (Exception exc) {
            Log.e(TAG, "Can not convert URL: "+exc.getMessage());
        }
        return uri;
    }

    /**
     * inner class for engine
     */
    class VideoEngine extends Engine {
        private static final String TAG = "VideoEngine";
        private SurfaceHolder mSurfaceHolder;

        public VideoEngine() {
            super();
            getSharedPreferencesData();
            mVideoUri = mVideoUrlToUri();
            Log.i(TAG, "[Video engine started]");
            if (mVideoUri != null) {
                try {
                    mMediaPlayer.setDataSource(getApplicationContext(), mVideoUri);
                    mMediaPlayer.prepareAsync();
                    mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

                        /**
                         * call when media player is ready
                         * @param mp
                         */
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            if (mSurfaceHolder != null) {
                                mp.setSurface(mSurfaceHolder.getSurface());
                                mp.setLooping(true);
                                mp.start();
                                Log.i(TAG, "media player is started");
                            } else {
                                Log.i(TAG, "Surface is not ready");
                            }
                        }
                    });

                    mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                        /**
                         * call when error fired
                         * @param mp
                         * @param what
                         * @param extra
                         * @return
                         */
                        @Override
                        public boolean onError(MediaPlayer mp, int what, int extra) {
                            Log.e(TAG, "[Error on media player]");
                            Log.e(TAG, "[mp] "+mp);
                            Log.e(TAG, "[what] "+what);
                            Log.e(TAG, "[extra] "+extra);
                            return false;
                        }
                    });

                    mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            Log.i(TAG, "media player is completed one loop");
                            mp.seekTo(0);
                        }
                    });


                } catch (Exception exc) {
                    Log.e(TAG,"Exception: "+exc.getMessage());
                }
            } else {
                Log.e(TAG,"video url is null");
            }
        }

        /**
         * call in visibility was changed
         * @param visible
         */
        @Override
        public void onVisibilityChanged(boolean visible) {
            if (visible) {
                Log.i(TAG, "Visibility true");
            } else {
                Log.i(TAG, "visibility false");
            }
        }

        /**
         * call when surface changed
         * @param holder
         * @param format
         * @param width
         * @param height
         */
        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height){
            super.onSurfaceChanged(holder, format, width, height);
            Log.i(TAG, "surface is changed");
        }

        /**
         * call when surface created
         * @param holder
         */
        @Override
        public void onSurfaceCreated(SurfaceHolder holder){
            this.mSurfaceHolder = holder;
            Log.i(TAG, "surface is created");
        }

        /**
         * call when surface destroyed
         * @param holder
         */
        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            if (mMediaPlayer != null) {
                mMediaPlayer.setLooping(isLooping);
            }
            Log.i(TAG, "surface is destroyed");
        }
    }
}
