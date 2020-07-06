package com.livewallrcandrapp.videowallpaper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;

import android.graphics.Canvas;
import android.graphics.Matrix;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.service.wallpaper.WallpaperService;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import java.io.IOException;


public class VideoWallpaperService extends WallpaperService {

    private static final String TAG = "VideoWallpaperService";

    /**
     * media player instances
     */
    private MediaPlayer mMediaPlayer = null;

    /**
     * broadcast receiver instance
     * for SCREEN_ON /OFF
     */
    private BroadcastReceiver mBroadcastReceiver = null;

    /**
     * instance for source url
     * both image and video
     */
    private Uri mVideoUri;

    /**
     * source url as string come from the shared data
     * both image and video
     */
    private String mVideoURL;

    /**
     * mime type of file
     * video:- video/mp4 or any other format
     * image:- image/png or any other format
     */
    private String mMimeType;

    /**
     * is video must need looping
     */
    private boolean isLooping;

    /**
     * is source video or image
     */
    private static boolean isVideo;

    /**
     * set image source to bitmap object
     */
    private Bitmap mImageWallpaper;

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
        mIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
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
            Log.e(TAG, "[onStartCommand] Exception error: "+exc.getMessage());
        }

        if (!screenOn) {
            if (mMediaPlayer != null && isVideo) {
                mMediaPlayer.start();
            }
        } else {
            if (mMediaPlayer != null && isVideo) {
                mMediaPlayer.seekTo(0);
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
            Log.i(TAG, "[onDestroy] media player stopped and release");
            if (mMediaPlayer != null && isVideo) {
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
            unregisterReceiver(mBroadcastReceiver);
        }
        Log.i(TAG, "[onDestroy] service is destroyed");
    }

    /**
     * check file type is video or image
     */
    private void checkFileType() {
        if (!mMimeType.equals(Utility.EMPTY)) {
            try {
                String[] words = mMimeType.split("/");
                if (words[0].equals("video")) {
                    isVideo = true;
                } else if(words[0].equals("image")) {
                    isVideo = false;
                } else {
                    Log.i(TAG, "[checkFileType] file type not matched");
                }
            } catch (Exception exc) {
                Log.e(TAG, "[checkFileType] exception error: "+exc.getMessage());
            }
        } else {
            Log.e(TAG, "[checkFileType] mime type is null");
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
            mMimeType = mSharedPreferences.getString(Utility.MIME_TYPE,Utility.EMPTY);
            Log.i(TAG,"mVideoURL : " +mVideoURL);
            Log.i(TAG, "isLooping : " +isLooping);
            Log.i(TAG, "mimeType : "+mMimeType);
        } catch (Exception exc) {
            Log.e(TAG, "[getSharedPreferencesData] error in getSharedPreferences: " +exc.getMessage());
        }

    }

    /**
     * get image from uri and resize
     */
    private void setImageWallpaperMatrix() {
        Bitmap bitmap = null;
        try {
            if (mVideoUri != null) {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), mVideoUri);
                mImageWallpaper = setScaleToScreenSize(bitmap);
            } else {
                Log.e(TAG, "[setImageWallpaperMatrix] URL is null");
            }
        } catch (IOException exc) {
            Log.e(TAG, "[setImageWallpaperMatrix] exception error: "+exc.getMessage());
        }
    }

    /**
     * resize bitmap to fit with device screen
     * @param original
     * @return
     */
    private Bitmap setScaleToScreenSize(Bitmap original) {
        Bitmap outputBitmap = null;
        try {
            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager mWindowManager = ((WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE));
            mWindowManager.getDefaultDisplay().getMetrics(metrics);

            int original_width = original.getWidth();
            int original_height = original.getHeight();

            int metrics_width = metrics.widthPixels;
            int metrics_height = metrics.heightPixels;

            float scaledWidth = (float) metrics_width/ (float) original_width;
            float scaledHeight = (float) metrics_height/ (float) original_height;

            //crate a matrix for scaled image
            Matrix matrix = new Matrix();
            matrix.postScale(scaledWidth, scaledHeight);

            //recreate bitmap
            outputBitmap = Bitmap.createBitmap(original, 0, 0, original_width, original_height, matrix, true);

        } catch (Exception exc) {
           Log.e(TAG, "[setScaleToScreenSize] exception error: "+exc.getMessage());
        }
        return outputBitmap;
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
            Log.e(TAG, "[mVideoUrlToUri] Can not convert URL: "+exc.getMessage());
        }
        return uri;
    }

    /**
     * inner class for engine
     */
    class VideoEngine extends Engine {
        private static final String TAG = "VideoEngine";
        private SurfaceHolder mSurfaceHolder;
        private final Handler mHandler = new Handler();
        private Runnable mDrawThread;
        private int mFrameRate = 20;
        private boolean mVisible;

        public VideoEngine() {
            super();
            // image/jpeg video/mp4
            getSharedPreferencesData();
            checkFileType();
            mVideoUri = mVideoUrlToUri();
            Log.i(TAG, "[Video engine started]");
            if (mVideoUri != null && isVideo) {
                Log.i(TAG, "Url is video");
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
                                mp.setVolume(0,0);
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
            } else if (mVideoUri != null && !isVideo) {
                Log.i(TAG, "Url is image");
                mDrawThread = new Runnable() {
                    @Override
                    public void run() {
                        setImageWallpaperMatrix();
                        try {
                            Thread.sleep(50);
                        } catch (Exception exc) {
                            Log.e(TAG, "[VideoEngine] exception error: "+exc.getMessage());
                        }
                        drawFrameToCanvas();
                    }
                };
            }else {
                Log.e(TAG,"video url is null");
            }
        }

        /**
         * get bitmap image and draw on the canvas
         */
        private void drawFrameToCanvas() {
            Canvas mCanvas = null;
            try {
                if (mSurfaceHolder == null) {
                    mSurfaceHolder = getSurfaceHolder();
                }
                if (mSurfaceHolder != null) {
                    mCanvas = mSurfaceHolder.lockCanvas();
                    if (mCanvas != null && mImageWallpaper != null) {
                        mCanvas.drawBitmap(mImageWallpaper, 00.0f,00.0f, null);
                    } else {
                        Log.e(TAG, "[drawFrameToCanvas] canvas is null or bitmap null");
                    }
                } else {
                    Log.e(TAG, "[drawFrameToCanvas] mSurfaceHolder is null");
                }
            } catch (Exception exc) {
                Log.e(TAG, "[drawFrameToCanvas] exception error: "+exc.getMessage());
            } finally {
                if (mCanvas != null && mSurfaceHolder != null) mSurfaceHolder.unlockCanvasAndPost(mCanvas);
            }

            if (mHandler != null) {
                mHandler.removeCallbacks(mDrawThread);
                if (mVisible) mHandler.postDelayed(mDrawThread, 1000/mFrameRate);
            } else {
                Log.e(TAG, "[drawFrameToCanvas] handler thread is null");
            }

        }



        /**
         * call in visibility was changed
         * @param visible
         */
        @Override
        public void onVisibilityChanged(boolean visible) {
            mVisible = visible;
            if (visible && !isVideo) {
                Log.i(TAG, "Visibility true");
                drawFrameToCanvas();
            } else if (!visible && !isVideo) {
                Log.i(TAG, "visibility false");
                if (mHandler != null && mDrawThread != null) mHandler.removeCallbacks(mDrawThread);
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
            if (!isVideo) drawFrameToCanvas();
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
         * call when off set ahs been changed
         * @param xOffset
         * @param yOffset
         * @param xStep
         * @param yStep
         * @param xPixels
         * @param yPixels
         */
        @Override
        public void onOffsetsChanged(float xOffset, float yOffset,
                                     float xStep, float yStep, int xPixels, int yPixels) {
            super.onOffsetsChanged(xOffset, yOffset,xStep, yStep,
                    xPixels, yPixels);
            if (!isVideo) drawFrameToCanvas();
        }


        /**
         * call when surface destroyed
         * @param holder
         */
        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            mVisible = false;
            if (mMediaPlayer != null && isVideo) {
                mMediaPlayer.setLooping(isLooping);
            } else if (!isVideo) {
                mHandler.removeCallbacks(mDrawThread);
            }
            Log.i(TAG, "surface is destroyed");
        }

        /**
         * call when class destroy
         */
        @Override
        public void onDestroy() {
            super.onDestroy();
            if (!isVideo && mDrawThread != null) mHandler.removeCallbacks(mDrawThread);
        }
    }
}
