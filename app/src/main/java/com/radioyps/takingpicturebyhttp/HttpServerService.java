package com.radioyps.takingpicturebyhttp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;

import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import static com.radioyps.takingpicturebyhttp.CommonConstants.DESTROY_ACITVITY;
import static com.radioyps.takingpicturebyhttp.CommonConstants.EXTRA_IMAGE_BYTE_ARRAY;
import static com.radioyps.takingpicturebyhttp.CommonConstants.MSG_ACTIVITY_READY;
import static com.radioyps.takingpicturebyhttp.CommonConstants.MSG_PICTURE_TAKEN;
import static com.radioyps.takingpicturebyhttp.CommonConstants.TAKING_PICTURE;


public  class HttpServerService  extends Service
{
    private static Handler mHandler;
    private static String LOG_TAG = "HttpServerService";


    private static Looper looper;

    private static Bitmap mBitmap = null;
    private static Context mContext = null;
    private static boolean requirePhoto = false;
    private static Thread httpServerThread = null;



    private  void getImage(Intent intent)
    {
        byte[] imageBytes = intent.getByteArrayExtra(EXTRA_IMAGE_BYTE_ARRAY);
       mBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
       AsyncHttpServerYep.setmBitmap(mBitmap);
        synchronized (httpServerThread) {
                httpServerThread.notify();

        }
    }

    public IBinder onBind(Intent paramIntent)
    {
        return null;
    }

    public void onCreate()
    {
        super.onCreate();
        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_PICTURE_TAKEN:
                        getImage((Intent) msg.obj);
                        //finish();
                        break;
                    case MSG_ACTIVITY_READY:
                        takePhoto();
                        break;
                }
            }};
        mContext=getBaseContext();
        Log.d(LOG_TAG, "onCreate() >>");
        AsyncHttpServerYep.startHttpServer(mContext, this);
        httpServerThread = AsyncHttpServerYep.getThread();


    }

    public int onStartCommand(Intent paramIntent, int paramInt1, int paramInt2)
    {
        super.onStartCommand(paramIntent, paramInt1, paramInt2);
        return START_STICKY;
    }


     public void requireTakingPhoto(){
         startActivity(new Intent(mContext, MainActivity.class ));
         requirePhoto = true;
     }

    public void requireStopTakingPhoto(){
        MainActivity.sendMessageToActivity(DESTROY_ACITVITY);
        requirePhoto = false;
    }

     public static void sendPictureToService(Intent intent ) {
         Message msg = mHandler.obtainMessage();
         msg.obj = intent;
         msg.what = MSG_PICTURE_TAKEN;
         mHandler.sendMessage(msg);
    }

    public static void informActivityReady( ) {
        if(mHandler != null){
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_ACTIVITY_READY;
            mHandler.sendMessage(msg);
        }

    }

    private void takePhoto(){
        if(requirePhoto){
            MainActivity.sendMessageToActivity(TAKING_PICTURE);
            requirePhoto = false;
        }
    }
}


