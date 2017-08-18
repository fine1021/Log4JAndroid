package org.apache.logging.log4j.core.appender.rolling;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;

import java.io.File;

/**
 * MonitorTriggeringPolicy
 */

@Plugin(name = "MonitorTriggeringPolicy", category = "Core", printObject = true)
public class MonitorTriggeringPolicy implements TriggeringPolicy {

    private static final String TAG = "MonitorTriggeringPolicy";

    /**
     * Allow subclasses access to the status logger without creating another instance.
     */
    protected static final Logger LOGGER = StatusLogger.getLogger();

    private RollingFileManager mManager;

    @Override
    public void initialize(RollingFileManager manager) {
        this.mManager = manager;
    }

    @Override
    public boolean isTriggeringEvent(LogEvent event) {
        String fileName = mManager.getFileName();
        final File file = new File(fileName);
        if (!file.exists()) {
            try {
                final File parent = file.getParentFile();
                if (null != parent && !parent.exists()) {
                    parent.mkdirs();
                }
                file.createNewFile();
            } catch (Exception ioe) {
                LOGGER.error("Unable to create file " + fileName, ioe);
                return false;
            }
            try {
                mManager.createFileAfterRollover();
            } catch (Exception e) {
                LOGGER.error("Unable to createFileAfterRollover " + fileName, e);
                return false;
            }
        }
        return false;
    }

    @PluginFactory
    public static MonitorTriggeringPolicy createPolicy() {
        return new MonitorTriggeringPolicy();
    }
}
