package com.radioyps.takingpicturebyhttp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import static com.radioyps.takingpicturebyhttp.CommonConstants.DESTROY_ACITVITY;
import static com.radioyps.takingpicturebyhttp.CommonConstants.EXTRA_IMAGE_BYTE_ARRAY;
import static com.radioyps.takingpicturebyhttp.CommonConstants.MSG_ACTIVITY_READY;
import static com.radioyps.takingpicturebyhttp.CommonConstants.MSG_ACTIVITY_READY_DELAY_5_SEC;
import static com.radioyps.takingpicturebyhttp.CommonConstants.MSG_PICTURE_READY_TO_SEND;
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
    private static PowerManager.WakeLock mWakeLock;
    private Notification mNotification = null;
    private final  int NOTIFICATION_ID = 10;
    IntentFilter intentfilter;




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
                    case MSG_PICTURE_READY_TO_SEND:
                        getImage((Intent) msg.obj);
                        //finish();
                        break;
                    case MSG_ACTIVITY_READY:
                        delayTakingPhoto();
                        break;
                    case MSG_ACTIVITY_READY_DELAY_5_SEC:
                        takePhoto();
                        break;

                }
            }};
        mContext=getBaseContext();
        Log.d(LOG_TAG, "onCreate() >>");
        AsyncHttpServerYep.startHttpServer(mContext, this);
        httpServerThread = AsyncHttpServerYep.getThread();
        PowerManager powerManager = (PowerManager)getSystemService(this.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "YEP:CameraTaking::wakeLock");
        setUpAsForeground("Attic leaking monitoring");
        intentfilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        this.registerReceiver(broadcastreceiver,intentfilter);


    }

    void setUpAsForeground(String text) {

        mNotification = makeNotification(text);
        startForeground(NOTIFICATION_ID, mNotification);
        Log.d(LOG_TAG, "setUpAsForeground()>> setUpAsForeground()");
    }


    private Notification makeNotification(String message) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

//        Notification notification =
//                new Notification.Builder(this, CHANNEL_DEFAULT_IMPORTANCE)
//                        .setContentTitle(getText(R.string.notification_title))
//                        .setContentText(getText(R.string.notification_message))
//                        .setSmallIcon(R.drawable.icon)
//                        .setContentIntent(pendingIntent)
//                        .setTicker(getText(R.string.ticker_text))
//                        .build();
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(13, notificationBuilder.build());/* ID of notification */
        return notificationBuilder.build();
    }


    private BroadcastReceiver broadcastreceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Integer batteryVol = (int)(intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE,0));
            Float fullVoltage = (float) (batteryVol * 0.001);
            Log.v(LOG_TAG, "Battery: voltage: " + fullVoltage);

        }
    };


    public int onStartCommand(Intent paramIntent, int paramInt1, int paramInt2)
    {
        super.onStartCommand(paramIntent, paramInt1, paramInt2);
        return START_STICKY;
    }


     public void requireTakingPhoto(){
         Intent intent = new Intent(mContext, MainActivity.class);
         intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         //intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
         startActivity(intent);
         requirePhoto = true;
     }

    public void requireStopTakingPhoto(){
        MainActivity.sendMessageToActivity(DESTROY_ACITVITY);
        requirePhoto = false;
    }

     public static void sendPictureToService(Intent intent ) {
         Message msg = mHandler.obtainMessage();
         msg.obj = intent;
         msg.what = MSG_PICTURE_READY_TO_SEND;
         mHandler.sendMessage(msg);
    }

    public static void informActivityReady( ) {
        if(mHandler != null){
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_ACTIVITY_READY;
            mHandler.sendMessage(msg);
        }

    }

    private void delayTakingPhoto(){
        Log.d(LOG_TAG, "delayTakingPhoto() >> delay in 5 sec");
        Message msg = mHandler.obtainMessage();
        msg.what = MSG_ACTIVITY_READY_DELAY_5_SEC;
        mHandler.sendMessageDelayed(msg,5000);
    }

    private void takePhoto(){

        if(requirePhoto){
            Log.d(LOG_TAG, "takePhoto() >>");
            MainActivity.sendMessageToActivity(TAKING_PICTURE);
            requirePhoto = false;
        }
    }
}


