package com.example.log4j2;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.log4j2.Log4jConfigurator;
import android.support.log4j2.appender.LogcatAppender;
import android.support.log4j2.slf4j.Log4jHook;
import android.support.log4j2.status.StatusLoggerHook;
import android.util.Log;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender;
import org.apache.logging.log4j.core.appender.rolling.CleanTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.MonitorTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.AndroidConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.status.StatusConfiguration;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Serializable;
import java.util.Map;


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
//                Log4jConfigurator.setConfiguration(getResources().openRawResource(R.raw.log4j2_all_logger_asynchronous));
                Log4jConfigurator.setConfiguration(new ProgrammaticConfiguration());
            } else {
                Log4jConfigurator.setConfiguration(getResources().openRawResource(R.raw.log4j2_logcat));
            }
        } else {
//            Log4jConfigurator.setConfiguration(getResources().openRawResource(R.raw.log4j2_all_logger_asynchronous));
            Log4jConfigurator.setConfiguration(new ProgrammaticConfiguration());
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
//            log4j2Configuration();
            log4j2ProgrammaticConfiguration();
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
            StatusLoggerHook.hookStatic();
            Log4jConfigurator.initStatic(getApplicationContext(), true);
            log4j2Configuration();
            sLogger.info("Log4j2 Initialization & Update work took {} ms", (System.currentTimeMillis() - t));
        }
    };

    /**
     * 必须通过{@link AbstractConfiguration#getRootLogger()}或者{@link AndroidConfiguration#getRootLogger()}加入{@link Appender}才能更新成功
     * <br>
     * 通过创建一个新的{@link LoggerConfig}，然后再调用{@link org.apache.logging.log4j.core.config.Configuration#addLogger(String, LoggerConfig)}，这种方式无法更新
     * <p>
     * 总结：直接更新{@link AbstractConfiguration#getRootLogger()}即可
     */
    private void log4j2ProgrammaticConfiguration() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();

        if (config instanceof AbstractConfiguration) {
            AbstractConfiguration abstractConfiguration = (AbstractConfiguration) config;
            config.getAppenders().clear();
            Map<String, LoggerConfig> map = config.getLoggers();
            if (!map.isEmpty()) {
                for (String s : map.keySet()) {
                    config.removeLogger(s);
                }
            }
            if (!abstractConfiguration.getRootLogger().getAppenders().isEmpty()) {
                for (String s : abstractConfiguration.getRootLogger().getAppenders().keySet()) {
                    abstractConfiguration.getRootLogger().removeAppender(s);
                }
            }
        } else if (config instanceof AndroidConfiguration) {
            AndroidConfiguration androidConfiguration = (AndroidConfiguration) config;
            androidConfiguration.getAppenders().clear();
            androidConfiguration.clearAppenders();
            androidConfiguration.clearLoggerConfigs();
        }

        Layout<? extends Serializable> logcatLayout = PatternLayout.newBuilder().withPattern("%m").withConfiguration(config).build();
        ThresholdFilter filter = ThresholdFilter.createFilter(Level.ALL, Filter.Result.ACCEPT, Filter.Result.DENY);
        LogcatAppender logcatAppender = LogcatAppender.createAppender("Logcat", false, logcatLayout, filter);
        logcatAppender.start();
        config.addAppender(logcatAppender);

        String packageName = getPackageName();
        String fileName = Environment.getExternalStorageDirectory() + File.separator +
                packageName + "/app_async.log";
        String filePattern = Environment.getExternalStorageDirectory() + File.separator +
                packageName + "/app_async-%d{yyyy-MM-dd}-%i.log.gz";
        String append = "true";
        String name = "RollingRandomAccessFile";
        String immediateFlush = "false";
        ThresholdFilter fileFilter = ThresholdFilter.createFilter(Level.DEBUG, Filter.Result.ACCEPT, Filter.Result.DENY);
        MonitorTriggeringPolicy monitorTriggeringPolicy = MonitorTriggeringPolicy.createPolicy();
        CleanTriggeringPolicy cleanTriggeringPolicy = CleanTriggeringPolicy.createPolicy("5");
        SizeBasedTriggeringPolicy sizeBasedTriggeringPolicy = SizeBasedTriggeringPolicy.createPolicy("5M");
        TimeBasedTriggeringPolicy timeBasedTriggeringPolicy = TimeBasedTriggeringPolicy.createPolicy("1", "false");
        CompositeTriggeringPolicy policies = CompositeTriggeringPolicy.createPolicy(monitorTriggeringPolicy,
                cleanTriggeringPolicy, sizeBasedTriggeringPolicy, timeBasedTriggeringPolicy);
        DefaultRolloverStrategy defaultRolloverStrategy = DefaultRolloverStrategy.createStrategy("20", null, null, null, config);
        Layout<? extends Serializable> fileLayout = PatternLayout.newBuilder().withPattern("%d %p %c{1} [%t] %m%n").withConfiguration(config).build();
        RollingRandomAccessFileAppender rollingRandomAccessFileAppender = RollingRandomAccessFileAppender.createAppender(
                fileName,
                filePattern,
                append,
                name,
                immediateFlush,
                null,
                policies,
                defaultRolloverStrategy,
                fileLayout,
                fileFilter,
                null, null, null, config);
        rollingRandomAccessFileAppender.start();
        config.addAppender(rollingRandomAccessFileAppender);

        if (config instanceof AbstractConfiguration) {
            AbstractConfiguration abstractConfiguration = (AbstractConfiguration) config;
            abstractConfiguration.getRootLogger().addAppender(logcatAppender, null, null);
            abstractConfiguration.getRootLogger().addAppender(rollingRandomAccessFileAppender, null, null);
            abstractConfiguration.getRootLogger().setLevel(Level.DEBUG);
        } else if (config instanceof AndroidConfiguration) {
            AndroidConfiguration androidConfiguration = (AndroidConfiguration) config;
            androidConfiguration.getRootLogger().addAppender(logcatAppender, null, null);
            androidConfiguration.getRootLogger().addAppender(rollingRandomAccessFileAppender, null, null);
            androidConfiguration.getRootLogger().setLevel(Level.DEBUG);
        }
        context.updateLoggers();
    }

    /**
     * 必须通过{@link AndroidConfiguration#getRootLogger()}加入{@link Appender}才能更新成功
     */
    private class ProgrammaticConfiguration extends AndroidConfiguration {

        protected ProgrammaticConfiguration() {
            super(ConfigurationSource.NULL_SOURCE);
            StatusConfiguration statusConfig = new StatusConfiguration();
            statusConfig.withStatus(Level.ERROR);
            statusConfig.initialize();
        }

        @Override
        protected void doConfigure() {
            //super.doConfigure();

            getAppenders().clear();
            clearAppenders();
            clearLoggerConfigs();

            setName("MyConfiguration");
            Layout<? extends Serializable> logcatLayout = PatternLayout.newBuilder().withPattern("%m").withConfiguration(this).build();
            ThresholdFilter filter = ThresholdFilter.createFilter(Level.ALL, Filter.Result.ACCEPT, Filter.Result.DENY);
            LogcatAppender logcatAppender = LogcatAppender.createAppender("Logcat", false, logcatLayout, filter);
            logcatAppender.start();
            addAppender(logcatAppender);

            String packageName = getPackageName();
            String fileName = Environment.getExternalStorageDirectory() + File.separator +
                    packageName + "/app_async.log";
            String filePattern = Environment.getExternalStorageDirectory() + File.separator +
                    packageName + "/app_async-%d{yyyy-MM-dd}-%i.log.gz";
            String append = "true";
            String name = "RollingRandomAccessFile";
            String immediateFlush = "false";
            ThresholdFilter fileFilter = ThresholdFilter.createFilter(Level.DEBUG, Filter.Result.ACCEPT, Filter.Result.DENY);
            MonitorTriggeringPolicy monitorTriggeringPolicy = MonitorTriggeringPolicy.createPolicy();
            CleanTriggeringPolicy cleanTriggeringPolicy = CleanTriggeringPolicy.createPolicy("5");
            SizeBasedTriggeringPolicy sizeBasedTriggeringPolicy = SizeBasedTriggeringPolicy.createPolicy("5M");
            TimeBasedTriggeringPolicy timeBasedTriggeringPolicy = TimeBasedTriggeringPolicy.createPolicy("1", "false");
            CompositeTriggeringPolicy policies = CompositeTriggeringPolicy.createPolicy(monitorTriggeringPolicy,
                    cleanTriggeringPolicy, sizeBasedTriggeringPolicy, timeBasedTriggeringPolicy);
            DefaultRolloverStrategy defaultRolloverStrategy = DefaultRolloverStrategy.createStrategy("20", null, null, null, this);
            Layout<? extends Serializable> fileLayout = PatternLayout.newBuilder().withPattern("%d %p %c{1} [%t] %m%n").withConfiguration(this).build();
            RollingRandomAccessFileAppender rollingRandomAccessFileAppender = RollingRandomAccessFileAppender.createAppender(
                    fileName,
                    filePattern,
                    append,
                    name,
                    immediateFlush,
                    null,
                    policies,
                    defaultRolloverStrategy,
                    fileLayout,
                    fileFilter,
                    null, null, null, this);
            rollingRandomAccessFileAppender.start();
            addAppender(rollingRandomAccessFileAppender);

            LoggerConfig root = getRootLogger();
            root.addAppender(logcatAppender, null, null);
            root.addAppender(rollingRandomAccessFileAppender, null, null);
            root.setLevel(Level.DEBUG);
        }
    }
}
