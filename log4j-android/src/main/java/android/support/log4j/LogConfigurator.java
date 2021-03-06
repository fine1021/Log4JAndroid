package android.support.log4j;

import org.apache.log4j.AsyncAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.helpers.LogLog;

import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * Configures the Log4j logging framework.
 */

public class LogConfigurator {

    private Level rootLevel = Level.DEBUG;
    private String filePattern = "%d - [%p::%c::%C] - %m%n";
    private String logCatPattern = "%m%n";
    private String fileName = "android-log4j.log";
    private int maxBackupSize = 10;
    private long maxFileSize = 512 * 1024;
    private boolean immediateFlush = true;
    private boolean useLogCatAppender = true;
    private boolean useFileAppender = true;
    private boolean resetConfiguration = true;
    private boolean internalDebugging = false;
    private String datePattern = "'.'yyyy-MM-dd";
    private boolean asyncFileAppender = true;
    private int maxBackupDays = -1;
    private boolean dailyRollingMode = true;

    public LogConfigurator() {
    }

    /**
     * @param fileName Name of the log file
     */
    public LogConfigurator(final String fileName) {
        setFileName(fileName);
    }

    /**
     * @param fileName  Name of the log file
     * @param rootLevel Log level for the root logger
     */
    public LogConfigurator(final String fileName, final Level rootLevel) {
        this(fileName);
        setRootLevel(rootLevel);
    }

    /**
     * @param fileName    Name of the log file
     * @param rootLevel   Log level for the root logger
     * @param filePattern Log pattern for the file appender
     */
    public LogConfigurator(final String fileName, final Level rootLevel, final String filePattern) {
        this(fileName);
        setRootLevel(rootLevel);
        setFilePattern(filePattern);
    }

    /**
     * @param fileName      Name of the log file
     * @param maxBackupSize Maximum number of backed up log files
     * @param maxFileSize   Maximum size of log file until rolling
     * @param filePattern   Log pattern for the file appender
     * @param rootLevel     Log level for the root logger
     */
    public LogConfigurator(final String fileName, final int maxBackupSize,
                           final long maxFileSize, final String filePattern, final Level rootLevel) {
        this(fileName, rootLevel, filePattern);
        setMaxBackupSize(maxBackupSize);
        setMaxFileSize(maxFileSize);
    }

    public void configure() {
        final Logger root = Logger.getRootLogger();

        if (isResetConfiguration()) {
            LogManager.getLoggerRepository().resetConfiguration();
        }

        LogLog.setInternalDebugging(isInternalDebugging());

        if (isUseFileAppender()) {
            configureFileAppender();
        }

        if (isUseLogCatAppender()) {
            configureLogCatAppender();
        }

        root.setLevel(getRootLevel());
    }

    /**
     * Sets the level of logger with name <code>loggerName</code>.
     * Corresponds to log4j.properties <code>log4j.logger.org.apache.what.ever=ERROR</code>
     *
     * @param loggerName the logger name
     * @param level      level
     */
    public void setLevel(final String loggerName, final Level level) {
        Logger.getLogger(loggerName).setLevel(level);
    }

    private void configureFileAppender() {
        final Logger root = Logger.getRootLogger();
        final Layout fileLayout = new PatternLayout(getFilePattern());

        if (isDailyRollingMode()) {
            final DailyRollingFileAppender dailyRollingFileAppender;
            try {
                dailyRollingFileAppender = new DailyRollingFileAppender(fileLayout, getFileName(), getDatePattern());
            } catch (final IOException e) {
                throw new RuntimeException("Exception configuring log system", e);
            }
            dailyRollingFileAppender.setImmediateFlush(isImmediateFlush());
            dailyRollingFileAppender.setMaximumFileSize(getMaxFileSize());
            dailyRollingFileAppender.setMaxBackupIndex(getMaxBackupSize());
            dailyRollingFileAppender.setMaxBackupDays(getMaxBackupDays());

            if (isAsyncFileAppender()) {
                AsyncAppender asyncAppender = new AsyncAppender();
                asyncAppender.addAppender(dailyRollingFileAppender);
                root.addAppender(asyncAppender);
            } else {
                root.addAppender(dailyRollingFileAppender);
            }
        } else {
            final RollingFileAppender rollingFileAppender;
            try {
                rollingFileAppender = new RollingFileAppender(fileLayout, getFileName());
            } catch (final IOException e) {
                throw new RuntimeException("Exception configuring log system", e);
            }
            rollingFileAppender.setMaxBackupIndex(getMaxBackupSize());
            rollingFileAppender.setMaximumFileSize(getMaxFileSize());
            rollingFileAppender.setImmediateFlush(isImmediateFlush());

            if (isAsyncFileAppender()) {
                AsyncAppender asyncAppender = new AsyncAppender();
                asyncAppender.addAppender(rollingFileAppender);
                root.addAppender(asyncAppender);
            } else {
                root.addAppender(rollingFileAppender);
            }
        }
    }

    private void configureLogCatAppender() {
        final Logger root = Logger.getRootLogger();
        final Layout logCatLayout = new PatternLayout(getLogCatPattern());
        final LogCatAppender logCatAppender = new LogCatAppender(logCatLayout);

        root.addAppender(logCatAppender);
    }

    /**
     * Return the log level of the root logger
     *
     * @return Log level of the root logger
     */
    public Level getRootLevel() {
        return rootLevel;
    }

    /**
     * Sets log level for the root logger
     *
     * @param level Log level for the root logger
     */
    public void setRootLevel(final Level level) {
        this.rootLevel = level;
    }

    public String getFilePattern() {
        return filePattern;
    }

    public void setFilePattern(final String filePattern) {
        this.filePattern = filePattern;
    }

    public String getLogCatPattern() {
        return logCatPattern;
    }

    public void setLogCatPattern(final String logCatPattern) {
        this.logCatPattern = logCatPattern;
    }

    /**
     * Returns the name of the log file
     *
     * @return the name of the log file
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Sets the name of the log file
     *
     * @param fileName Name of the log file
     */
    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    /**
     * Returns the maximum number of backed up log files
     *
     * @return Maximum number of backed up log files
     */
    public int getMaxBackupSize() {
        return maxBackupSize;
    }

    /**
     * Sets the maximum number of backed up log files
     *
     * @param maxBackupSize Maximum number of backed up log files
     */
    public void setMaxBackupSize(final int maxBackupSize) {
        this.maxBackupSize = maxBackupSize;
    }

    /**
     * Returns the maximum size of log file until rolling
     *
     * @return Maximum size of log file until rolling
     */
    public long getMaxFileSize() {
        return maxFileSize;
    }

    /**
     * Sets the maximum size of log file until rolling
     *
     * @param maxFileSize Maximum size of log file until rolling
     */
    public void setMaxFileSize(final long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public boolean isImmediateFlush() {
        return immediateFlush;
    }

    public void setImmediateFlush(final boolean immediateFlush) {
        this.immediateFlush = immediateFlush;
    }

    /**
     * Returns true, if FileAppender is used for logging
     *
     * @return True, if FileAppender is used for logging
     */
    public boolean isUseFileAppender() {
        return useFileAppender;
    }

    /**
     * @param useFileAppender the useFileAppender to set
     */
    public void setUseFileAppender(final boolean useFileAppender) {
        this.useFileAppender = useFileAppender;
    }

    /**
     * Returns true, if LogcatAppender should be used
     *
     * @return True, if LogcatAppender should be used
     */
    public boolean isUseLogCatAppender() {
        return useLogCatAppender;
    }

    /**
     * If set to true, LogCatAppender will be used for logging
     *
     * @param useLogCatAppender If true, LogCatAppender will be used for logging
     */
    public void setUseLogCatAppender(final boolean useLogCatAppender) {
        this.useLogCatAppender = useLogCatAppender;
    }

    public void setResetConfiguration(boolean resetConfiguration) {
        this.resetConfiguration = resetConfiguration;
    }

    /**
     * Resets the log4j configuration before applying this configuration. Default is true.
     *
     * @return True, if the log4j configuration should be reset before applying this configuration.
     */
    public boolean isResetConfiguration() {
        return resetConfiguration;
    }

    public void setInternalDebugging(boolean internalDebugging) {
        this.internalDebugging = internalDebugging;
    }

    public boolean isInternalDebugging() {
        return internalDebugging;
    }

    /**
     * Returns the value of the <b>DatePattern</b> option.
     *
     * @return datePattern
     */
    public String getDatePattern() {
        return datePattern;
    }

    /**
     * The <b>DatePattern</b> takes a string in the same format as
     * expected by {@link SimpleDateFormat}. This options determines the
     * rollover schedule. used by {@link DailyRollingFileAppender}
     *
     * @param datePattern datePattern
     */
    public void setDatePattern(String datePattern) {
        this.datePattern = datePattern;
    }

    public boolean isAsyncFileAppender() {
        return asyncFileAppender;
    }

    public void setAsyncFileAppender(boolean asyncFileAppender) {
        this.asyncFileAppender = asyncFileAppender;
    }

    /**
     * Returns the value of the <b>MaxBackupDays</b> option.
     *
     * @return maxBackupDays
     */
    public int getMaxBackupDays() {
        return maxBackupDays;
    }

    /**
     * Set the maximum days the log should keep, <tt>-1</tt> means keep all the logs
     *
     * @param maxBackupDays maxBackupDays
     */
    public void setMaxBackupDays(int maxBackupDays) {
        this.maxBackupDays = maxBackupDays;
    }

    /**
     * Returns the value of the <b>DailyRollingMode</b> option.
     *
     * @return dailyRollingMode
     */
    public boolean isDailyRollingMode() {
        return dailyRollingMode;
    }

    /**
     * Set the dailyRollingMode, it only valid when {@link #isUseFileAppender()} return true
     *
     * @param dailyRollingMode If set to {@code true}, {@link DailyRollingFileAppender} will be used for logging,
     *                         otherwise {@link RollingFileAppender} will be used for logging
     */
    public void setDailyRollingMode(boolean dailyRollingMode) {
        this.dailyRollingMode = dailyRollingMode;
    }
}
