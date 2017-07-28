package android.support.log4j2.slf4j;

import org.slf4j.impl.StaticLoggerBinder;

import java.lang.reflect.Field;

/**
 * Created by yexiaokang on 2017/7/28.
 */

public final class Log4jHook {


    public static void hookStatic() {
        try {
            Field field = StaticLoggerBinder.class.getDeclaredField("loggerFactory");
            field.setAccessible(true);
            field.set(StaticLoggerBinder.getSingleton(), new Log4jLoggerFactory());
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
