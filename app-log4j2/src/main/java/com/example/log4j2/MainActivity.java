package com.example.log4j2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static Logger sLogger;
    private Timer mTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            sLogger = LoggerFactory.getLogger(TAG);
            sLogger.info("before onPostPermissionCheckCompleted");
            onPostPermissionCheckCompleted();
        }
    }

    private void onPermissionCheckCompleted() {
        if (Build.VERSION.SDK_INT >= 23) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AppGlobal.getInstance().applyLog4j2Update();
                    sLogger = LoggerFactory.getLogger(TAG);
                    sLogger.info("onPermissionCheckCompleted");
                    onPostPermissionCheckCompleted();
                }
            });
        }
    }

    private void onPostPermissionCheckCompleted() {
        testLog4j();
    }

    private void testLog4j() {
        mTimer = new Timer("Log4j2Test");
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                sLogger.debug("Life is sad at times, but it is up to you to make your own life happy.");
            }
        }, 0, 1000);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onPermissionCheckCompleted();
            } else {
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mTimer != null) {
            mTimer.cancel();
        }
        super.onDestroy();
    }
}
