package com.radioyps.takingpicturebyhttp;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
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



public  class HttpServerService  extends Service
{
    private static Handler mHandler;
    private static String LOG_TAG = "HttpServerService";
    public final static String EXTRA_IMAGE_BYTE_ARRAY = "com.radioyps.takingpicturebyhttp.HttpServerService";
    public static final int MSG_PICTURE_TAKEN = 0x188;
    private static Looper looper;
    private static Thread httpCameraServerThread;

    private void cleanupAndShutdown()
    {
        Handler localHandler = this.mHandler;
        if (localHandler != null)
        {
            localHandler.removeCallbacksAndMessages(null);
            this.mHandler = null;
        }
        stopSelf();
    }



    public IBinder onBind(Intent paramIntent)
    {
        return null;
    }

    public void onCreate()
    {
        super.onCreate();
        Handler localHandler = new Handler();
        this.mHandler = localHandler;
        Log.d(LOG_TAG, "onCreate() >>");
        httpCameraServerThread.startHttpServer();
        new Runnable()
        {
            public void run()
            {
                if (HttpServerService.this.mHandler == null) {
                    return;
                }
                if (true)
                {
                    Intent localIntent = new Intent(HttpServerService.this.getBaseContext(), MainActivity.class);
                    localIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    localIntent.putExtra("goto", true);
                    HttpServerService.this.startActivity(localIntent);
                    HttpServerService.this.cleanupAndShutdown();
                    return;
                }
                HttpServerService.this.mHandler.postDelayed(this, 1000L);
            }
        }.run();
        localHandler.postDelayed(new Runnable()
        {
            public void run()
            {
                HttpServerService.this.cleanupAndShutdown();
            }
        }, 60000L);
    }

    public int onStartCommand(Intent paramIntent, int paramInt1, int paramInt2)
    {
        super.onStartCommand(paramIntent, paramInt1, paramInt2);
        return START_STICKY;
    }

     /* Create Http Server */

    private static class HttpCameraServerThread implements Runnable {


        private static AsyncHttpServer mAsyncHttpServer = null;
        private static HttpServerRequestCallback mServerCb = null;


        private HttpCameraServerThread() {

        }

        @Override
        public void run() {

            Looper.prepare();
            looper = Looper.myLooper();
            Log.d(LOG_TAG, "HttpCameraServerThread 0 >>");

            mAsyncHttpServer = new AsyncHttpServer() {
                protected boolean onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                    Log.i(LOG_TAG, request.getHeaders().toString());
                    return super.onRequest(request, response);
                }
            };

            Log.d(LOG_TAG, "HttpCameraServerThread 1 >>");
            mServerCb = new HttpServerRequestCallback() {

                public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                    response.getHeaders().set("Cache-Control", "no-cache");

                    try {
                        //takingPhoto();/* this action take time to finish */

                        Log.d(LOG_TAG, "HttpCameraServerThread:  onRequest()>>  ");

                        synchronized (httpCameraServerThread) {
                            try {
                                httpCameraServerThread.wait();
                            } catch (InterruptedException e) {

                            }
                        }

                        ByteArrayOutputStream localByteArrayOutputStream = new ByteArrayOutputStream();

                        mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, localByteArrayOutputStream);
                        localByteArrayOutputStream.flush();
                        response.send("image/jpeg", localByteArrayOutputStream.toByteArray());
                        return;

                    } catch (Exception e) {
                        response.code(500);
                        response.send(e.toString());
                    }
                }
            };
            Log.d(LOG_TAG, "Main.HttpServer 2 >>");
            mAsyncHttpServer.get("/screenshot.jpg", mServerCb);
            Log.d(LOG_TAG, "Main.HttpServer is running()>>");
            mAsyncHttpServer.listen(8888);


        }

        /**
         * Entry point.
         */
        public static void startHttpServer() {
            HttpCameraServerThread wrapper = new HttpCameraServerThread();
            httpCameraServerThread = new Thread(wrapper, "HttpCameraServerThread");
            httpCameraServerThread.start();
            //th.join();

        }
    }

     public static void sendPictureToService(Intent intent ) {
         Message msg = mHandler.obtainMessage();
         msg.obj = intent;
         msg.what = MSG_PICTURE_TAKEN;
         mHandler.sendMessage(msg);


    }

}


