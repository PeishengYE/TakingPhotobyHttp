package com.radioyps.takingpicturebyhttp;

import android.app.AlarmManager;
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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

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
    private static long TIME_INTERVAL = 35*1000;
    private static long TIME_DELAY = 3*1000;
    private Handler mHandlerCopyBateryLevel = null;
    /* every BATTERY_LEVEL_COPY_INTERVAL, copy it to one day map */
    private final  int BATTERY_LEVEL_COPY_INTERVAL = 5;
    /* in second */
    private final  int COPY_HANDLER_TIME_INTERVAL = 60;

    public static Map<String, String> batteryLevelOneHour;
    static {
        batteryLevelOneHour = new HashMap<>();

    }
    public static String latestBatteryLevel;
    public static Map<String, String> batteryLevelOneDay;
    static {
        batteryLevelOneDay = new HashMap<>();

    }

    private void copyBatteryLevel(){

        for(Map.Entry<String, String> entry: batteryLevelOneHour.entrySet()){
                      batteryLevelOneDay.put(entry.getKey(), entry.getValue());
                      break;

        }

        if(batteryLevelOneDay.size() >= 3600){
            batteryLevelOneDay.clear();
        }
    }

    private void startCopyBatteryLevel(){
        if(mHandlerCopyBateryLevel == null){
            mHandlerCopyBateryLevel = new Handler();
            mHandlerCopyBateryLevel.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.v(LOG_TAG, "copy battery Level start");
                    copyBatteryLevel();
                    mHandlerCopyBateryLevel.postDelayed(this, COPY_HANDLER_TIME_INTERVAL*1000);
                }
            }, COPY_HANDLER_TIME_INTERVAL*1000);
        }


    }

    private  void getImage(Intent intent)
    {
        byte[] imageBytes = intent.getByteArrayExtra(EXTRA_IMAGE_BYTE_ARRAY);
       mBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
       String time = makeTime();
       String tmp = "Date: " + time + " Battery Voltage: " + latestBatteryLevel;
        mBitmap = drawTextToBitmap(tmp, mBitmap);
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

        startCopyBatteryLevel();

        PowerManager powerManager = (PowerManager)getSystemService(this.POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "YEP:CameraTaking::wakeLock");
        setUpAsForeground("Attic leaking monitoring");
        intentfilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        this.registerReceiver(systemBatteryStatusReceiver,intentfilter);

        intentfilter = new IntentFilter("com.radioyps.takingpicturebyhttp.HEARTBEAT");
        this.registerReceiver(wakeUPtoTakingPhoto,intentfilter);
        setAlarm(mContext);

    }

    private String makeTime(){
        Long currentTimeMillis = System.currentTimeMillis();
        SimpleDateFormat format = new SimpleDateFormat("MMdd_HHmm");
        return format.format(currentTimeMillis);
    }


    public Bitmap drawTextToBitmap(
                                   String gText,
                                   Bitmap image) {




        android.graphics.Bitmap.Config bitmapConfig =
                image.getConfig();
        // set default bitmap config if none
        if(bitmapConfig == null) {
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
        }
        // resource bitmaps are imutable,
        // so we need to convert it to mutable one
        image = image.copy(bitmapConfig, true);

        Canvas canvas = new Canvas(image);
        // new antialised Paint
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        // text color - #3D3D3D
        paint.setColor(Color.rgb(61, 61, 61));
        // text size in pixels
        paint.setTextSize((int) (14 * 2));
        // text shadow
        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);

        // draw text to the Canvas center
        Rect bounds = new Rect();
        paint.getTextBounds(gText, 0, gText.length(), bounds);
        int x = (image.getWidth() - bounds.width())/2;
        int y = (image.getHeight() + bounds.height())/2 + 400 ;
        /* Drawing X: 891 Y: 552 */
        Log.v(LOG_TAG, "Drawing X: " + x  + " Y: " + y);

        canvas.drawText(gText, x, y, paint);

        return image;
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


    private BroadcastReceiver systemBatteryStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Integer batteryVol = (int)(intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE,0));
            Float fullVoltage = (float) (batteryVol * 0.001);
            String tmp = "Battery: voltage: " + fullVoltage + "\n";
            if (batteryLevelOneHour.size()> 3600){
                batteryLevelOneHour.clear();
            }
            batteryLevelOneHour.put(makeTime(), fullVoltage.toString());
            latestBatteryLevel = fullVoltage.toString();
            Log.v(LOG_TAG, tmp);
//            saveDataInFile( tmp);

        }
    };

    private BroadcastReceiver wakeUPtoTakingPhoto = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(LOG_TAG, "wakeUPtoTakingPhoto()>> ");
            String time = makeTime();
            makeNotification("Wake UP on " + time);
//            requireTakingPhoto();

        }
    };

    public void setAlarm(Context context) {
        //Toast.makeText(context, R.string.updating_in_progress, Toast.LENGTH_LONG).show(); // For example
        Log.d(LOG_TAG, "Set alarm!");
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intnt = new Intent(context, AlarmReceiver.class);
        PendingIntent pendngIntnt = PendingIntent.getBroadcast(context, 0, intnt, 0);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + TIME_DELAY, TIME_INTERVAL, pendngIntnt);
    }



    private void saveDataInFile(String data){
        try {

            File folder = new File(this.getExternalFilesDir(null), "BatteryLevel");
            folder.mkdirs();

            File batteryFile = new File(folder, "batteryLevel.txt");
//            FileOutputStream stream = new FileOutputStream(batteryFile);
            FileOutputStream fOut = openFileOutput("batteryLevel.txt", MODE_APPEND);
            OutputStreamWriter osw = new OutputStreamWriter(fOut);
            osw.write(data);
            osw.flush();
            osw.close();


        } catch (Exception e){
            e.printStackTrace();
        }
    }




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


