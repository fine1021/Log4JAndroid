package com.yxkang.android.log4jandroid.application;

import android.app.Application;
import android.os.Environment;

/**
 * Created by fine on 2016/1/27.
 */
public class Log4JApplication extends Application {

    private static Log4JApplication log4JApplication = null;

    @Override
    public void onCreate() {
        super.onCreate();
        log4JApplication = this;
        log4jConfigure(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED));
    }

    public static Log4JApplication getInstance() {
        if (log4JApplication == null) {
            log4JApplication = new Log4JApplication();
        }
        return log4JApplication;
    }

    public synchronized void log4jConfigure(boolean sdcardExist) {

    }
}
