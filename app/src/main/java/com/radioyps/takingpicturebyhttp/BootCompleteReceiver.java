package com.radioyps.takingpicturebyhttp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class BootCompleteReceiver
  extends BroadcastReceiver
{
    private static final String LOG_TAG = "<<YEP inside>>: " + BootCompleteReceiver.class.getSimpleName();

  public void onReceive(Context mContext, Intent paramIntent)
  {
    String toastStr = paramIntent.getStringExtra("toast");
    Log.d(LOG_TAG,"prepare for toast()>> ");

    if (!TextUtils.isEmpty(toastStr)) {
      Toast.makeText(mContext, "TakingPciture: BootComplete", Toast.LENGTH_LONG).show();
    }
    mContext.startService(new Intent(mContext, HttpServerService.class));
  }


}

