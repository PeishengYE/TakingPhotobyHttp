package com.radioyps.takingpicturebyhttp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

import static com.radioyps.takingpicturebyhttp.CommonConstants.DESTROY_ACITVITY;
import static com.radioyps.takingpicturebyhttp.CommonConstants.EXTRA_IMAGE_BYTE_ARRAY;
import static com.radioyps.takingpicturebyhttp.CommonConstants.SERVICE_IS_READY;
import static com.radioyps.takingpicturebyhttp.CommonConstants.TAKING_PICTURE;
import static com.radioyps.takingpicturebyhttp.HttpServerService.informActivityReady;
import static com.radioyps.takingpicturebyhttp.HttpServerService.sendPictureToService;

public class MainActivity extends AppCompatActivity {

    private static Handler mHandler;
    private CameraView cv;
    //准备一个相机对象
    private static Camera mCamera = null;
    //准备一个Bitmap对象
    private static Bitmap mBitmap = null;
    private static SurfaceHolder holder = null;
    private static Context mContext = null;
    private static boolean isServerReady = false;


    private static void sendPictureToServiceLocal(byte[] data){
        Intent intent = new Intent(mContext, HttpServerService.class);
        intent.putExtra(EXTRA_IMAGE_BYTE_ARRAY, data);
        //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        sendPictureToService(intent);
    }
    //准备一个保存图片的PictureCallback对象
    public static Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {

        public void onPictureTaken(byte[] data, Camera camera) {
            Log.i("YEP: CameraTaking","onPictureTaken: done on taking picture >>> 1" );
            //Toast.makeText(getApplicationContext(), "正在保存……", Toast.LENGTH_LONG).show();
            //用BitmapFactory.decodeByteArray()方法可以把相机传回的裸数据转换成Bitmap对象

            mBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            Log.i("YEP: CameraTaking","onPictureTaken: done on taking picture >>> 2");
//            synchronized (httpCameraServerThread) {
//                httpCameraServerThread.notify();
//            }

            //接下来的工作就是把Bitmap保存成一个存储卡中的文件
            File file = new File("/sdcard/YEP"+ new DateFormat().format("yyyyMMdd_hhmmss", Calendar.getInstance(Locale.CHINA)) + ".png");
            try {
                file.createNewFile();
                BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file));
                mBitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.flush();
                os.close();
                //Toast.makeText(getApplicationContext(), "图片保存完毕，在存储卡的根目录", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                e.printStackTrace();
            }
            sendPictureToServiceLocal(data);
            // 停止预览
            mCamera.stopPreview();
            // 释放相机资源并置空
            mCamera.release();
            mCamera = null;
            //finish();
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getBaseContext();

        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case DESTROY_ACITVITY:
                        finish();
                        break;
                    case TAKING_PICTURE:
                        takingPhoto();
                        break;
                    case SERVICE_IS_READY:
                        isServerReady = true;
                        break;

                }
            }};

        //窗口去掉标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //窗口设置为全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //设置窗口为半透明
        getWindow().setFormat(PixelFormat.TRANSLUCENT);

        //提供一个帧布局
        FrameLayout fl = new FrameLayout(this);

        //创建一个照相预览用的SurfaceView子类，并放在帧布局的底层
        Log.i("YEP: CameraTaking","onCreate()>> creating a surface view..");
        cv = new CameraView(this);
        fl.addView(cv);

        //创建一个文本框添加在帧布局中，我们可以看到，文字自动出现在了SurfaceView的前面，由此你可以在预览窗口做出各种特殊效果
        TextView tv = new TextView(this);
        tv.setText("YEP Camera Test");
        fl.addView(tv);

        //设置Activity的根内容视图
        setContentView(fl);
        startService(new Intent(this, HttpServerService.class));
        informActivityReady();
    }


    private static  void takingPhoto(){

        Log.i("YEP: CameraTaking","takingPhoto()>>");
        if(mCamera == null){
            Log.i("YEP: CameraTaking","mCamera reopen...");
            mCamera = Camera.open();
            try {
                //设置预览
                mCamera.setPreviewDisplay(holder);
            } catch (IOException e) {
                // 释放相机资源并置空
                mCamera.release();
                mCamera = null;
            }
        }

        if (mCamera != null) {
            Log.i("YEP: CameraTaking","mCamera.takePicture");
            //获得相机参数对象
            Camera.Parameters parameters = mCamera.getParameters();
            //设置格式
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
            parameters.setPictureFormat(PixelFormat.JPEG);
            //设置预览大小，这里我的测试机是Milsstone所以设置的是854x480
            //parameters.setPreviewSize(2560, 1440);
            //设置自动对焦
            parameters.setFocusMode("auto");
            //设置图片保存时的分辨率大小
            parameters.setPictureSize(1920, 1080);
            //给相机对象设置刚才设定的参数
            mCamera.setParameters(parameters);
            //当按下相机按钮时，执行相机对象的takePicture()方法,该方法有三个回调对象做入参，不需要的时候可以设null
            mCamera.takePicture(null, null, pictureCallback);
        }else
            Log.i("YEP: CameraTaking","mCamera is null, abort !");

        Log.i("YEP: CameraTaking","takingPhoto()<<");

    }

    public static void sendMessageToActivity(int what ){
        String mesg = null;
        Message.obtain(mHandler,
                what,
                mesg).sendToTarget();
    }

    // 照相视图
    class CameraView extends SurfaceView {



        //构造函数
        public CameraView(Context context) {
            super(context);
            Log.i("","CameraView");

            // 操作surface的holder
            holder = this.getHolder();
            // 创建SurfaceHolder.Callback对象
            holder.addCallback(new SurfaceHolder.Callback() {

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    // 停止预览
                    if(mCamera != null)  mCamera.stopPreview();
                    // 释放相机资源并置空
                    releaseCamera();
                }

                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    //当预览视图创建的时候开启相机
                    mCamera = Camera.open();
                    try {
                        //设置预览
                        mCamera.setPreviewDisplay(holder);
                    } catch (IOException e) {
                        // 释放相机资源并置空
                        releaseCamera();
                    }

                }

                //当surface视图数据发生变化时，处理预览信息
                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

                    try {
                        Log.i("YEP: CameraTaking","surfaceChanged()>>");
                        //获得相机参数对象
                        Camera.Parameters parameters = mCamera.getParameters();
                        //设置格式
                        parameters.setPictureFormat(PixelFormat.JPEG);
                        //设置预览大小，这里我的测试机是Milsstone所以设置的是854x480
                        parameters.setPreviewSize(800, 480);
                        //设置自动对焦
                        parameters.setFocusMode("auto");
                        //设置图片保存时的分辨率大小
                        parameters.setPictureSize(800, 480);
                        //给相机对象设置刚才设定的参数
                        mCamera.setParameters(parameters);
                        //开始预览
                        mCamera.startPreview();
                        Log.i("YEP: CameraTaking","surfaceChanged()<<");
                    } catch (IllegalStateException e){
                        Log.i("YEP: CameraTaking","failed on setParameters");
                        e.printStackTrace();
                        releaseCamera();
                    }
                }
            });
            // 设置Push缓冲类型，说明surface数据由其他来源提供，而不是用自己的Canvas来绘图，在这里是由摄像头来提供数据
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

    }

    private void releaseCamera(){

        if(mCamera != null){

            mCamera.release();
            mCamera = null;
        }

    }

}
