package android.support.log4j2.status;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.status.StatusConfiguration;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.apache.logging.log4j.status.StatusConsoleListener;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * StatusLoggerHook
 */

public class StatusLoggerHook {

    private static final EmptyStatusListener EMPTY_STATUS_LISTENER = new EmptyStatusListener();

    /**
     * in log4j {@link XmlConfiguration} constructor,
     * {@link StatusConfiguration#initialize()} might add a {@link StatusConsoleListener} to {@link StatusLogger},
     * so before we start configuration, add a {@link StatusConsoleListener} to avoid adding another one
     * <br>
     * if {@link StatusLogger} has listeners, it will deliver the log messages to these listeners
     */
    public static void hookStatic() {
        StatusLogger.getLogger().registerListener(EMPTY_STATUS_LISTENER);
    }

    private static class EmptyStatusListener extends StatusConsoleListener {

        public EmptyStatusListener() {
            super(Level.OFF);
        }

        @Override
        public Level getStatusLevel() {
            return Level.OFF;
        }

        @Override
        public void log(StatusData data) {
            // empty, no-op
        }
    }
}
