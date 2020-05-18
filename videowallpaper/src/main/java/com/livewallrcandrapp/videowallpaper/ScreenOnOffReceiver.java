package com.livewallrcandrapp.videowallpaper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ScreenOnOffReceiver extends BroadcastReceiver {

    private static final String TAG = "ScreenOnOffReceiver";
    private boolean screenOFF;

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                Log.i(TAG, "[Screen is OFF]");
               screenOFF = true;
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                Log.i(TAG, "Screen is ON");
                screenOFF = false;
            }
        } catch (Exception exc) {
            Log.i(TAG,"Exception in [ScreenOnOffReceiver]: "+exc.getMessage());
        }

        Intent mIntent = new Intent(context, VideoWallpaperService.class);
        mIntent.putExtra("screen_state", screenOFF);
        Log.i(TAG, "calling wallpaper service from broadcast receiver");
        context.startService(mIntent);
    }
}
