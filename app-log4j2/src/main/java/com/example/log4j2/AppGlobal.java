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

public class AppGlobal extends Application implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "AppGlobal";
    private static Logger sLogger;
    private static AppGlobal sAppGlobal;

    private Thread.UncaughtExceptionHandler mDefaultHandler;
    private HandlerThread mHandlerThread;
    private Handler mHandler;

    public static AppGlobal getInstance() {
        if (sAppGlobal == null) {
            throw new IllegalStateException("Unbelievable? Application is null");
        }
        return sAppGlobal;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sAppGlobal = this;
        long t = System.currentTimeMillis();
        Log4jHook.hookStatic();
        Log.i(TAG, "onCreate: slf4j hookStatic took " + (System.currentTimeMillis() - t) + "ms");

        t = System.currentTimeMillis();
        Log4jConfigurator.initEnv(true);
        Log.i(TAG, "onCreate: log4j2 initEnv took " + (System.currentTimeMillis() - t) + "ms");

        t = System.currentTimeMillis();
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(AppGlobal.this);
        mHandler = new Handler(mHandlerThread.getLooper());
        mHandler.post(mLog4j2InitRunnable);
        Log.i(TAG, "onCreate: remaining work took " + (System.currentTimeMillis() - t) + "ms");
    }

    public void applyLog4j2Update() {
        mHandler.post(mLog4j2UpdateRunnable);
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
        if (sLogger != null) {
            sLogger.error(Log.getStackTraceString(ex));
            return true;
        } else {
            Log.e(TAG, "handleException: ", ex);
            return false;
        }
    }

    /**
     * 更新日志配置的时候，如果{@code ContextSelector}发生变化的话，
     * 之前已经使用其他的{@code ContextSelector}初始化的日志就不会同步了，
     * 所以必须保持{@code ContextSelector}一致
     */
    private void log4j2Configuration() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, android.os.Process.myPid(),
                    android.os.Process.myUid()) == PackageManager.PERMISSION_GRANTED) {
                Log4jConfigurator.setConfiguration(getResources().openRawResource(R.raw.log4j2_all_logger_asynchronous));
            } else {
                Log4jConfigurator.setConfiguration(getResources().openRawResource(R.raw.log4j2_logcat));
            }
        } else {
            Log4jConfigurator.setConfiguration(getResources().openRawResource(R.raw.log4j2_all_logger_asynchronous));
        }
        if (sLogger == null) {
            sLogger = LoggerFactory.getLogger(TAG);
        }
    }

    /**
     * {@code Log4j2}更新日志配置任务
     */
    private Runnable mLog4j2UpdateRunnable = new Runnable() {
        @Override
        public void run() {
            long t = System.currentTimeMillis();
            log4j2Configuration();
            sLogger.info("Log4j2 Update work took {} ms", (System.currentTimeMillis() - t));
        }
    };

    /**
     * {@code Log4j2}初始化和日志配置任务
     */
    private Runnable mLog4j2InitRunnable = new Runnable() {
        @Override
        public void run() {
            long t = System.currentTimeMillis();
//            StatusLoggerHook.hookStatic();
            Log4jConfigurator.initStatic(getApplicationContext(), true);
            log4j2Configuration();
            sLogger.info("Log4j2 Initialization & Update work took {} ms", (System.currentTimeMillis() - t));
        }
    };
}
