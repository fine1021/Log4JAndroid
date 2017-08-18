package org.apache.logging.log4j.core.appender.rolling;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;

import java.io.File;
import java.util.Date;

/**
 * CleanTriggeringPolicy
 */

@Plugin(name = "CleanTriggeringPolicy", category = "Core", printObject = true)
public class CleanTriggeringPolicy implements TriggeringPolicy {

    private static final String TAG = "CleanTriggeringPolicy";

    /**
     * Allow subclasses access to the status logger without creating another instance.
     */
    protected static final Logger LOGGER = StatusLogger.getLogger();

    private static final int DAY_OF_MILLIS = 24 * 60 * 60 * 1000;
    private static final int MAX_TRIGGER_TIME = 2;

    private long maxBackupDay;
    private long maxBackupPeriod;
    private long rolloverInterval;
    private long nextRolloverMillis = 0;

    private int mTriggerTime = 0;

    private RollingFileManager manager;

    protected CleanTriggeringPolicy(long maxBackupDay) {
        if (maxBackupDay > 0) {
            this.maxBackupDay = maxBackupDay;
            this.maxBackupPeriod = maxBackupDay * DAY_OF_MILLIS;
            this.rolloverInterval = maxBackupPeriod - DAY_OF_MILLIS;
        } else {
            this.maxBackupDay = -1;
            this.maxBackupPeriod = 0;
            this.rolloverInterval = 0;
        }
        LOGGER.debug(toString());
    }

    @Override
    public void initialize(RollingFileManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean isTriggeringEvent(LogEvent event) {
        long millis = event.getTimeMillis();
        if (maxBackupPeriod > 0 && (millis - nextRolloverMillis) >= maxBackupPeriod) {
            String fileName = manager.getFileName();
            LOGGER.debug("fileName = {}", fileName);

            StringBuilder buf = new StringBuilder();
            manager.getPatternProcessor().formatFileName(buf, "");
            String current = buf.toString();
            LOGGER.debug("current fileName prefix = {}", current);

            Date date = new Date(millis - rolloverInterval);
            buf.setLength(0);
            manager.getPatternProcessor().formatFileName(buf, date, "");
            String delete = buf.toString();
            LOGGER.debug("delete fileName prefix = {}", delete);

            if (current.equals(delete)) {
                // not support day date rollover, no need do anything
                this.maxBackupDay = -1;
                this.maxBackupPeriod = 0;
                this.rolloverInterval = 0;
                return false;
            }
            if (mTriggerTime < MAX_TRIGGER_TIME) {
                LOGGER.debug("delete triggerTime = {}", mTriggerTime);
                mTriggerTime++;
                return false;
            }
            File file = new File(fileName);
            File parent = file.getParentFile();
            if (parent.exists()) {
                LOGGER.debug("parentFile = {}", parent.getAbsolutePath());
                File[] files = parent.listFiles();
                if (files.length > 0) {
                    for (File f : files) {
                        String fName = f.getAbsolutePath();
                        if (fName.equals(fileName)) {
                            continue;
                        }
                        LOGGER.debug("candidate fileName = {}", f.getName());
                        if (fName.compareTo(delete) < 0) {
                            LOGGER.info("delete fileName = {}", f.delete());
                        }
                    }
                }
            }
            nextRolloverMillis = millis - rolloverInterval;
            mTriggerTime = 0;
        }
        return false;
    }

    @PluginFactory
    public static CleanTriggeringPolicy createPolicy(@PluginAttribute("maxBackupDay") final String maxBackupDay) {
        long value = Long.valueOf(maxBackupDay);
        return new CleanTriggeringPolicy(value);
    }

    @Override
    public String toString() {
        return "CleanTriggeringPolicy{" +
                "maxBackupDay=" + maxBackupDay +
                ", maxBackupPeriod=" + maxBackupPeriod +
                '}';
    }
}
