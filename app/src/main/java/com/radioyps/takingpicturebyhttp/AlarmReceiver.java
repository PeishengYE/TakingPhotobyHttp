package com.radioyps.takingpicturebyhttp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = AlarmReceiver.class.getSimpleName();
    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(TAG, "alarm receiver()>> keep System on");
        context.sendBroadcast(new Intent("com.radioyps.takingpicturebyhttp.HEARTBEAT"));

    }
}