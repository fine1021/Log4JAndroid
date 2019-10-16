package android.support.log4j;

import android.util.Log;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Appender for {@link Log}
 *
 * @author Rolf Kulemann, Pascal Bockhorn
 */

public class LogCatAppender extends AppenderSkeleton {

    /**
     * Max tag length enforced by Android
     * http://developer.android.com/reference/android/util/Log.html#isLoggable(java.lang.String, int)
     */
    private static final int MAX_TAG_LENGTH = 23;
    private static final int MAX_LOG_LENGTH = 1024 * 3;

    protected Layout tagLayout;

    public LogCatAppender(final Layout messageLayout, final Layout tagLayout) {
        this.tagLayout = tagLayout;
        setLayout(messageLayout);
    }

    public LogCatAppender(final Layout messageLayout) {
        this(messageLayout, new PatternLayout("%c"));
    }

    public LogCatAppender() {
        this(new PatternLayout("%m%n"));
    }

    @Override
    protected void append(final LoggingEvent le) {
        switch (le.getLevel().toInt()) {
            case Level.TRACE_INT:
                if (le.getThrowableInformation() != null) {
                    log(Log.VERBOSE, getTag(le), getLayout().format(le) + '\n'
                            + Log.getStackTraceString(le.getThrowableInformation().getThrowable()));
                } else {
                    log(Log.VERBOSE, getTag(le), getLayout().format(le));
                }
                break;
            case Level.DEBUG_INT:
                if (le.getThrowableInformation() != null) {
                    log(Log.DEBUG, getTag(le), getLayout().format(le) + '\n'
                            + Log.getStackTraceString(le.getThrowableInformation().getThrowable()));
                } else {
                    log(Log.DEBUG, getTag(le), getLayout().format(le));
                }
                break;
            case Level.INFO_INT:
                if (le.getThrowableInformation() != null) {
                    log(Log.INFO, getTag(le), getLayout().format(le) + '\n'
                            + Log.getStackTraceString(le.getThrowableInformation().getThrowable()));
                } else {
                    log(Log.INFO, getTag(le), getLayout().format(le));
                }
                break;
            case Level.WARN_INT:
                if (le.getThrowableInformation() != null) {
                    log(Log.WARN, getTag(le), getLayout().format(le) + '\n'
                            + Log.getStackTraceString(le.getThrowableInformation().getThrowable()));
                } else {
                    log(Log.WARN, getTag(le), getLayout().format(le));
                }
                break;
            case Level.ERROR_INT:
                if (le.getThrowableInformation() != null) {
                    log(Log.ERROR, getTag(le), getLayout().format(le) + '\n'
                            + Log.getStackTraceString(le.getThrowableInformation().getThrowable()));
                } else {
                    log(Log.ERROR, getTag(le), getLayout().format(le));
                }
                break;
            case Level.FATAL_INT:
                if (le.getThrowableInformation() != null) {
                    Log.wtf(getTag(le), getLayout().format(le), le.getThrowableInformation().getThrowable());
                } else {
                    Log.wtf(getTag(le), getLayout().format(le));
                }
                break;
        }
    }

    @Override
    public void close() {
    }

    @Override
    public boolean requiresLayout() {
        return true;
    }

    public Layout getTagLayout() {
        return tagLayout;
    }

    public void setTagLayout(final Layout tagLayout) {
        this.tagLayout = tagLayout;
    }

    private String getTag(LoggingEvent event) {
        String tag = getTagLayout().format(event);
        if ((tag.length() > MAX_TAG_LENGTH)) {
            tag = tag.substring(0, MAX_TAG_LENGTH - 1) + "*";
        }
        return tag;
    }

    private void log(int priority, String tag, String msg) {
        if (msg.length() > MAX_LOG_LENGTH) {
            int length = msg.length();
            for (int fromIndex = 0; fromIndex <= length; fromIndex += MAX_LOG_LENGTH) {
                int toIndex = Math.min(fromIndex + MAX_LOG_LENGTH, length);
                String subString = msg.substring(fromIndex, toIndex);
                Log.println(priority, tag, subString);
            }
        } else {
            Log.println(priority, tag, msg);
        }
    }
}

