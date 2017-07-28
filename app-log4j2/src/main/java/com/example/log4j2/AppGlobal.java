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
import android.support.log4j2.status.StatusLoggerHook;
import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by fine on 2017/7/28.
 */

public class AppGlobal extends Application implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "AppGlobal";
    private Thread.UncaughtExceptionHandler mDefaultHandler;
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
        long t = System.currentTimeMillis();
        Log4jHook.hookStatic();
        Log.i(TAG, "onCreate: slf4j hookStatic took " + (System.currentTimeMillis() - t) + "ms");

        t = System.currentTimeMillis();
        Log4jConfigurator.initEnv();
        Log.i(TAG, "onCreate: log4j2 initEnv took " + (System.currentTimeMillis() - t) + "ms");

        Handler handler = new Handler(mHandlerThread.getLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                StatusLoggerHook.hookStatic();
                /*
                 * 更新日志配置的时候，如果{@link ContextSelector}发生变化的话，
                 * 之前已经初始化的日志就不会同步了，所以要保持{@link ContextSelector}一致
                 */
                long t = System.currentTimeMillis();
                if (Build.VERSION.SDK_INT >= 23) {
                    if (checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, android.os.Process.myPid(),
                            android.os.Process.myUid()) == PackageManager.PERMISSION_GRANTED) {
                        Log4jConfigurator.initStatic(getApplicationContext(), true,
                                getResources().openRawResource(R.raw.log4j2_all_logger_asynchronous));
                    } else {
                        Log4jConfigurator.initStatic(getApplicationContext(), false,
                                getResources().openRawResource(R.raw.log4j2_logcat));
                    }
                } else {
                    Log4jConfigurator.initStatic(getApplicationContext(), true,
                            getResources().openRawResource(R.raw.log4j2_all_logger_asynchronous));
                }
                sLogger = LoggerFactory.getLogger(TAG);
                sLogger.info("Log4j2 Initialization work took {} ms", (System.currentTimeMillis() - t));
                mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
                Thread.setDefaultUncaughtExceptionHandler(AppGlobal.this);
                sLogger.debug("HandlerThread quit, for it has completed it's job");
                if (Build.VERSION.SDK_INT >= 18) {
                    mHandlerThread.quitSafely();
                } else {
                    mHandlerThread.quit();
                }
            }
        });
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (!handleException(e) && mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(t, e);
        } else {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    private boolean handleException(Throwable ex) {
        Log.e(TAG, "handleException: ", ex);
        sLogger.error(Log.getStackTraceString(ex));
        return true;
    }
}
