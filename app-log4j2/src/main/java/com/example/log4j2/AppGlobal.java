package com.example.log4j2;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.log4j2.Log4jConfigurator;
import android.support.log4j2.slf4j.Log4jHook;
import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by fine on 2017/7/28.
 */

public class AppGlobal extends Application {

    private static final String TAG = "AppGlobal";
    private HandlerThread mHandlerThread;
    private static Logger sLogger;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        long time = System.currentTimeMillis();
        Log4jHook.hookStatic();
        Log.i(TAG, "onCreate: slf4j hookStatic took " + (System.currentTimeMillis() - time) + "ms");

        Handler handler = new Handler(mHandlerThread.getLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                long t = System.currentTimeMillis();
                if (Build.VERSION.SDK_INT >= 23) {
                    if (checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, android.os.Process.myPid(),
                            android.os.Process.myUid()) == PackageManager.PERMISSION_GRANTED) {
                        Log4jConfigurator.initStatic(getApplicationContext(), false,
                                getResources().openRawResource(R.raw.log4j2_normal));
                    } else {
                        Log4jConfigurator.initStatic(getApplicationContext(), false,
                                getResources().openRawResource(R.raw.log4j2_logcat));
                    }
                } else {
                    Log4jConfigurator.initStatic(getApplicationContext(), false,
                            getResources().openRawResource(R.raw.log4j2_normal));
                }
                sLogger = LoggerFactory.getLogger(TAG);
                sLogger.info("Log4j2 Initialization work took {} ms", (System.currentTimeMillis() - t));
                sLogger.debug("HandlerThread quit, for it has completed it's job");
                if (Build.VERSION.SDK_INT >= 18) {
                    mHandlerThread.quitSafely();
                } else {
                    mHandlerThread.quit();
                }
            }
        });
    }
}
