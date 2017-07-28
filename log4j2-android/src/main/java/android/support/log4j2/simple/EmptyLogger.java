package android.support.log4j2.simple;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.simple.SimpleLogger;
import org.apache.logging.log4j.util.PropertiesUtil;

import java.io.PrintStream;

/**
 * Created by fine on 2017/7/29.
 */

public class EmptyLogger extends SimpleLogger {

    public EmptyLogger(String name, Level defaultLevel, boolean showLogName, boolean showShortLogName, boolean showDateTime, boolean showContextMap, String dateTimeFormat, MessageFactory messageFactory, PropertiesUtil props, PrintStream stream) {
        super(name, defaultLevel, showLogName, showShortLogName, showDateTime, showContextMap, dateTimeFormat, messageFactory, props, stream);
    }

    @Override
    public void logMessage(String fqcn, Level level, Marker marker, Message msg, Throwable throwable) {
        // empty, no-op
    }
}
