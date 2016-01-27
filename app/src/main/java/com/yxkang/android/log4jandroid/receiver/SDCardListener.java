package com.yxkang.android.log4jandroid.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.yxkang.android.log4jandroid.application.Log4JApplication;

/**
 * Created by fine on 2016/1/27.
 */
public class SDCardListener extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
            Log4JApplication.getInstance().log4jConfigure(true);
        } else {
            Log4JApplication.getInstance().log4jConfigure(false);
        }
    }
}
