package com.example.log4j2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.log4j2.Log4jConfigurator;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final Logger LOGGER = LoggerFactory.getLogger(TAG);
    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LOGGER.info("onCreate");
        checkPermission();
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            onPostPermissionCheckCompleted();
        }
    }

    private void onPermissionCheckCompleted() {
        if (Build.VERSION.SDK_INT >= 23) {
            Log4jConfigurator.initStatic(getApplicationContext(), false,
                    getResources().openRawResource(R.raw.log4j2_normal));
        }
        LOGGER.info("onPermissionCheckCompleted");
    }

    private void onPostPermissionCheckCompleted() {
        testLog4j();
    }

    private void testLog4j() {
        timer = new Timer("Log4j2Test");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                LOGGER.debug("Life is sad at times, but it is up to you to make your own life happy.");
            }
        }, 0, 1000);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onPermissionCheckCompleted();
                    }
                });
            } else {
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (timer != null) {
            timer.cancel();
        }
        super.onDestroy();
    }
}
