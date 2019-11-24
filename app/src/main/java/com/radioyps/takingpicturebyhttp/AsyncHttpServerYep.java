package com.radioyps.takingpicturebyhttp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Looper;
import android.util.Log;

import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import java.io.ByteArrayOutputStream;

import static com.radioyps.takingpicturebyhttp.CommonConstants.DESTROY_ACITVITY;
import static com.radioyps.takingpicturebyhttp.CommonConstants.TAKING_PICTURE;

/**
 * Created by yep on 24/11/19.
 */

     /* Create Http Server */

public  class AsyncHttpServerYep implements Runnable {


    private AsyncHttpServer mAsyncHttpServer = null;
    private HttpServerRequestCallback mServerCb = null;
    private static String LOG_TAG = "HttpServerThreadRunnable";
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


        Log.d(LOG_TAG, "HttpServerThreadRunnable 0 >>");

        mAsyncHttpServer = new AsyncHttpServer() {
            protected boolean onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                Log.i(LOG_TAG, request.getHeaders().toString());
                return super.onRequest(request, response);
            }
        };

        Log.d(LOG_TAG, "HttpServerThreadRunnable 1 >>");
        mServerCb = new HttpServerRequestCallback() {

            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                response.getHeaders().set("Cache-Control", "no-cache");

                try {


                    Log.d(LOG_TAG, "HttpServerThreadRunnable:  onRequest()>>  ");

                    mServerService.requireTakingPhoto();
                    synchronized (httpServerThread) {
                        try {
                            httpServerThread.wait();
                        } catch (InterruptedException e) {

                        }
                    }

                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                    os.flush();
                    response.send("image/jpeg", os.toByteArray());
                    mServerService.requireStopTakingPhoto();
                    return;

                } catch (Exception e) {
                    response.code(500);
                    response.send(e.toString());
                }
            }
        };
        Log.d(LOG_TAG, "HttpServer 2 >>");
        mAsyncHttpServer.get("/takingPhoto", mServerCb);
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