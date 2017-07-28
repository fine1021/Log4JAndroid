package android.support.log4j2.status;

import android.support.log4j2.simple.EmptyLogger;
import android.util.Log;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.Strings;

import java.lang.reflect.Field;

/**
 * StatusLoggerHook
 */

public class StatusLoggerHook {

    private static final String TAG = "StatusLoggerHook";
    private static final PropertiesUtil PROPS = new PropertiesUtil("log4j2.StatusLogger.properties");

    public static void hookStatic() {

        StatusLogger.getLogger();
        try {
            StatusLogger.getLogger();
            Field field = StatusLogger.class.getDeclaredField("logger");
            field.setAccessible(true);
            EmptyLogger logger = new EmptyLogger("StatusLogger", Level.ERROR, false, true, false,
                    false, Strings.EMPTY, null, PROPS, System.err);
            field.set(StatusLogger.getLogger(), logger);
            Log.d(TAG, "hookStatic: set EmptyLogger OK");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
