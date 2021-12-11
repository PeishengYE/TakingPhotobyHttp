package com.radioyps.takingpicturebyhttp;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.google.gson.Gson;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import java.io.ByteArrayOutputStream;



/**
 * Created by yep on 24/11/19.
 */

     /* Create Http Server */

public  class AsyncHttpServerYep implements Runnable {


    private AsyncHttpServer mAsyncHttpServer = null;
    private HttpServerRequestCallback mTakingPhotoCb = null;
    private HttpServerRequestCallback mGetBatteryLevelOneHourCb = null;
    private HttpServerRequestCallback mGetBatteryLevelOneDayCb = null;
    private static String LOG_TAG = "AsyncHttpServerYep";
    private static Context mContext = null;
    private static Thread httpServerThread;
    private static Bitmap mBitmap = null;
    private HttpServerService mServerService;

    private AsyncHttpServerYep(Context context, HttpServerService serverService) {
        mContext = context;
        mServerService= serverService;

    }


    @Override
    public void run() {


        //Log.d(LOG_TAG, "HttpServerThreadRunnable 0 >>");

        mAsyncHttpServer = new AsyncHttpServer() {
            protected boolean onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                Log.i(LOG_TAG, request.getHeaders().toString());
                return super.onRequest(request, response);
            }
        };

        //Log.d(LOG_TAG, "HttpServerThreadRunnable 1 >>");
        mTakingPhotoCb = new HttpServerRequestCallback() {

            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                response.getHeaders().set("Cache-Control", "no-cache");

                try {


                    Log.d(LOG_TAG, "onRequest()>> wait... ");

                    mServerService.requireTakingPhoto();
                    synchronized (httpServerThread) {
                        try {
                            httpServerThread.wait(20*1000);
                        } catch (InterruptedException e) {
                            Log.d(LOG_TAG, "onRequest()>> waiting timeout on taking photo from mainactivity... ");
                            response.code(500);
                            response.send("waiting timeout on taking photo from mainactivity");
                            mServerService.requireStopTakingPhoto();
                            return;
                        }
                    }
                    Log.d(LOG_TAG, "onRequest()>> continue... ");
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                    os.flush();
                    response.send("image/jpeg", os.toByteArray());
                    mServerService.requireStopTakingPhoto();
                    Log.d(LOG_TAG, "onRequest()>>  done ");
                    return;

                } catch (Exception e) {
                    response.code(500);
                    response.send(e.toString());
                }
            }
        };

        mGetBatteryLevelOneHourCb = new HttpServerRequestCallback() {

            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                response.getHeaders().set("Cache-Control", "no-cache");
                Gson gson = new Gson();
                String tmp = gson.toJson(HttpServerService.batteryLevelOneHour);
                response.send(tmp);

            }


        };

        mGetBatteryLevelOneDayCb = new HttpServerRequestCallback() {

            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                response.getHeaders().set("Cache-Control", "no-cache");
                Gson gson = new Gson();
                String tmp = gson.toJson(HttpServerService.batteryLevelOneDay);
                response.send(tmp);

            }


        };

//        Log.d(LOG_TAG, "HttpServer 2 >>");
        mAsyncHttpServer.get("/takingPhoto", mTakingPhotoCb);
        mAsyncHttpServer.get("/batteryLevelOneHour", mGetBatteryLevelOneHourCb);
        mAsyncHttpServer.get("/batteryLevelOneDay", mGetBatteryLevelOneDayCb);
        Log.d(LOG_TAG, "HttpServer is running()>>");
        mAsyncHttpServer.listen(8888);


    }

    public static  Thread getThread() {
      return httpServerThread;
    }

    public static void setmBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
    }
    /**
     * Entry point.
     */
    public static void startHttpServer(Context context, HttpServerService serverService) {
        AsyncHttpServerYep wrapper = new AsyncHttpServerYep(context,serverService);
        httpServerThread = new Thread(wrapper, "HttpServerThreadRunnable");
        httpServerThread.start();
        //th.join();

    }
}