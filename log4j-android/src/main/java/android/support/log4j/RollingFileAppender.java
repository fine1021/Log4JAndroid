package android.support.log4j;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.helpers.CountingQuietWriter;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.LoggingEvent;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Writer;

/**
 * RollingFileAppender extends FileAppender to backup the log files when
 * they reach a certain size.
 * <p>
 * The log4j extras companion includes alternatives which should be considered
 * for new deployments and which are discussed in the documentation
 * for org.apache.log4j.rolling.RollingFileAppender.
 */

public class RollingFileAppender extends FileAppender {

    /**
     * The default maximum file size is 10MB.
     */
    protected long maxFileSize = 10 * 1024 * 1024;

    /**
     * There is one backup file by default.
     */
    protected int maxBackupIndex = 1;

    private long nextRollover = 0;

    /**
     * The default constructor simply calls its {@link
     * FileAppender#FileAppender parents constructor}.
     */
    public RollingFileAppender() {
        super();
    }

    /**
     * Instantiate a RollingFileAppender and open the file designated by
     * <code>filename</code>. The opened filename will become the output
     * destination for this appender.
     * <p>
     * <p>If the <code>append</code> parameter is true, the file will be
     * appended to. Otherwise, the file desginated by
     * <code>filename</code> will be truncated before being opened.
     */
    public RollingFileAppender(Layout layout, String filename, boolean append)
            throws IOException {
        super(layout, filename, append);
    }

    /**
     * Instantiate a FileAppender and open the file designated by
     * <code>filename</code>. The opened filename will become the output
     * destination for this appender.
     * <p>
     * <p>The file will be appended to.
     */
    public RollingFileAppender(Layout layout, String filename) throws IOException {
        super(layout, filename);
    }

    /**
     * Returns the value of the <b>MaxBackupIndex</b> option.
     */
    public int getMaxBackupIndex() {
        return maxBackupIndex;
    }

    /**
     * Get the maximum size that the output file is allowed to reach
     * before being rolled over to backup files.
     *
     * @since 1.1
     */
    public long getMaximumFileSize() {
        return maxFileSize;
    }

    /**
     * Implements the usual roll over behaviour.
     * <p>
     * <p>If <code>MaxBackupIndex</code> is positive, then files
     * {<code>File.1</code>, ..., <code>File.MaxBackupIndex -1</code>}
     * are renamed to {<code>File.2</code>, ...,
     * <code>File.MaxBackupIndex</code>}. Moreover, <code>File</code> is
     * renamed <code>File.1</code> and closed. A new <code>File</code> is
     * created to receive further log output.
     * <p>
     * <p>If <code>MaxBackupIndex</code> is equal to zero, then the
     * <code>File</code> is truncated with no backup files created.
     */
    public void rollOver() {
        File target;
        File file;

        if (qw != null) {
            long size = ((CountingQuietWriter) qw).getCount();
            LogLog.debug("rolling over count=" + size);
            //   if operation fails, do not roll again until
            //      maxFileSize more bytes are written
            nextRollover = size + maxFileSize;
        }
        LogLog.debug("maxBackupIndex=" + maxBackupIndex);

        boolean renameSucceeded = true;
        // If maxBackups <= 0, then there is no file renaming to be done.
        if (maxBackupIndex > 0) {
            // Delete the oldest file, to keep Windows happy.
            file = new File(fileName + '.' + maxBackupIndex);
            if (file.exists())
                renameSucceeded = file.delete();

            // Map {(maxBackupIndex - 1), ..., 2, 1} to {maxBackupIndex, ..., 3, 2}
            for (int i = maxBackupIndex - 1; i >= 1 && renameSucceeded; i--) {
                file = new File(fileName + "." + i);
                if (file.exists()) {
                    target = new File(fileName + '.' + (i + 1));
                    LogLog.debug("Renaming file " + file + " to " + target);
                    renameSucceeded = file.renameTo(target);
                }
            }

            if (renameSucceeded) {
                // Rename fileName to fileName.1
                target = new File(fileName + "." + 1);

                this.closeFile(); // keep windows happy.

                file = new File(fileName);
                LogLog.debug("Renaming file " + file + " to " + target);
                renameSucceeded = file.renameTo(target);
                //
                //   if file rename failed, reopen file with append = true
                //
                if (!renameSucceeded) {
                    try {
                        this.setFile(fileName, true, bufferedIO, bufferSize);
                    } catch (IOException e) {
                        if (e instanceof InterruptedIOException) {
                            Thread.currentThread().interrupt();
                        }
                        LogLog.error("setFile(" + fileName + ", true) call failed.", e);
                    }
                }
            }
        }

        //
        //   if all renames were successful, then
        //
        if (renameSucceeded) {
            try {
                // This will also close the file. This is OK since multiple
                // close operations are safe.
                this.setFile(fileName, false, bufferedIO, bufferSize);
                nextRollover = 0;
            } catch (IOException e) {
                if (e instanceof InterruptedIOException) {
                    Thread.currentThread().interrupt();
                }
                LogLog.error("setFile(" + fileName + ", false) call failed.", e);
            }
        }
    }

    public synchronized void setFile(String fileName, boolean append, boolean bufferedIO, int bufferSize)
            throws IOException {
        super.setFile(fileName, append, this.bufferedIO, this.bufferSize);
        if (append) {
            if (qw != null) {
                File f = new File(fileName);
                ((CountingQuietWriter) qw).setCount(f.length());
            }
        }
    }


    /**
     * Set the maximum number of backup files to keep around.
     * <p>
     * <p>The <b>MaxBackupIndex</b> option determines how many backup
     * files are kept before the oldest is erased. This option takes
     * a positive integer value. If set to zero, then there will be no
     * backup files and the log file will be truncated when it reaches
     * <code>MaxFileSize</code>.
     */
    public void setMaxBackupIndex(int maxBackups) {
        this.maxBackupIndex = maxBackups;
    }

    /**
     * Set the maximum size that the output file is allowed to reach
     * before being rolled over to backup files.
     * <p>
     *
     * @see #setMaxFileSize(String)
     */
    public void setMaximumFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }


    /**
     * Set the maximum size that the output file is allowed to reach
     * before being rolled over to backup files.
     * <p>
     * <p>In configuration files, the <b>MaxFileSize</b> option takes an
     * long integer in the range 0 - 2^63. You can specify the value
     * with the suffixes "KB", "MB" or "GB" so that the integer is
     * interpreted being expressed respectively in kilobytes, megabytes
     * or gigabytes. For example, the value "10KB" will be interpreted
     * as 10240.
     */
    public void setMaxFileSize(String value) {
        maxFileSize = OptionConverter.toFileSize(value, maxFileSize + 1);
    }

    protected void setQWForFiles(Writer writer) {
        this.qw = new CountingQuietWriter(writer, errorHandler);
    }

    /**
     * This method differentiates RollingFileAppender from its super
     * class.
     *
     * @since 0.9.0
     */
    protected void subAppend(LoggingEvent event) {
        // if file not exists, create a new file
        File file = new File(fileName);
        if (!file.exists()) {
            try {
                setFile(fileName, true, bufferedIO, bufferSize);
            } catch (IOException ignored) {
            }
        }
        // no need write log if qw is null
        if (qw != null) {
            super.subAppend(event);
        }
        if (fileName != null && qw != null) {
            long size = ((CountingQuietWriter) qw).getCount();
            if (size >= maxFileSize && size >= nextRollover) {
                rollOver();
            }
        }
    }
}
