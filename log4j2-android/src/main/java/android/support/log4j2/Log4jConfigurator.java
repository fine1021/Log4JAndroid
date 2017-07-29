package android.support.log4j2;

import android.content.Context;
import android.os.Environment;
import android.support.log4j2.appender.LogcatAppender;
import android.support.log4j2.lookup.AndroidLookup;
import android.support.log4j2.selector.AndroidContextSelector;
import android.util.Log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.config.plugins.processor.PluginEntry;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.apache.logging.log4j.core.config.plugins.util.PluginRegistry;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.core.config.plugins.util.ResolverUtil;
import org.apache.logging.log4j.core.config.xml.XmlConfigurationFactory;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.selector.ClassLoaderContextSelector;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.spi.LoggerContextFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by yexiaokang on 2017/7/27.
 */

public class Log4jConfigurator {

    private static final String TAG = "Log4jConfigurator";
    private static final String PACKAGE_NAME = "android.support.log4j";
    /**
     * Name of the system property to use to identify the ContextSelector Class.
     */
    public static final String LOG4J_CONTEXT_SELECTOR = "Log4jContextSelector";
    /**
     * Android context selector
     */
    private static final String ANDROID_CONTEXT_SELECTOR = "android.support.log4j2.selector.AndroidContextSelector";
    /**
     * Default AsyncLoggerContextSelector
     */
    private static final String ASYNC_CONTEXT_SELECTOR = "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector";


    /**
     * When not invoke {@link LogManager#setFactory(LoggerContextFactory)} to set a {@link LoggerContextFactory},
     * log4j system will use {@link Log4jContextFactory#createContextSelector()} to create a default context selector.
     * if not set env context selector, it will use {@link ClassLoaderContextSelector} as default,
     * which maybe cause {@link NullPointerException}
     *
     * @param asyncLogger if set true, use {@link org.apache.logging.log4j.core.async.AsyncLogger} to print log,
     *                    which is Low-Latency logging, used for high-throughput work.
     *                    if use {@link org.apache.logging.log4j.core.appender.AsyncAppender} or normal logging,
     *                    this flags should set to false.
     */
    public static void initEnv(boolean asyncLogger) {
        // disable JMX on Android
        System.setProperty("log4j2.disable.jmx", "true");
        // set default env context selector.
        if (asyncLogger) {
            System.setProperty(LOG4J_CONTEXT_SELECTOR, ASYNC_CONTEXT_SELECTOR);
        } else {
            System.setProperty(LOG4J_CONTEXT_SELECTOR, ANDROID_CONTEXT_SELECTOR);
        }
    }

    /**
     * init log4j2 and inject plugins used by Android
     *
     * @param context     the Android context
     * @param asyncLogger if set true, use {@link org.apache.logging.log4j.core.async.AsyncLogger} to print log,
     *                    which is Low-Latency logging, used for high-throughput work.
     *                    if use {@link org.apache.logging.log4j.core.appender.AsyncAppender} or normal logging,
     *                    this flags should set to false.
     */
    public static void initStatic(Context context, boolean asyncLogger) {
        File file = context.getFilesDir();
        if (file != null) {
            AndroidLookup.getLookUpMap().put(AndroidLookup.FILES_DIR, file.getAbsolutePath());
        }
        file = context.getExternalFilesDir(null);
        if (file != null) {
            AndroidLookup.getLookUpMap().put(AndroidLookup.EXTERNAL_FILES_DIR, file.getAbsolutePath());
        }
        file = context.getExternalFilesDir("logs");
        if (file != null) {
            AndroidLookup.getLookUpMap().put(AndroidLookup.EXTERNAL_LOGS_DIR, file.getAbsolutePath());
        }
        file = Environment.getExternalStorageDirectory();
        if (file != null) {
            AndroidLookup.getLookUpMap().put(AndroidLookup.EXTERNAL_STORAGE_DIR, file.getAbsolutePath());
        }
        if (asyncLogger) {
            LogManager.setFactory(new Log4jContextFactory(new AsyncLoggerContextSelector()));
        } else {
            LogManager.setFactory(new Log4jContextFactory(new AndroidContextSelector()));
        }
        injectPlugins(PACKAGE_NAME, new Class<?>[]{AndroidLookup.class, LogcatAppender.class});
    }

    /**
     * Configuration the log4j system
     *
     * @param is the xml stream
     */
    public static void setConfiguration(InputStream is) {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        try {
            ConfigurationSource source = new ConfigurationSource(is);
            Configuration config = XmlConfigurationFactory.getInstance().getConfiguration(source);
            context.start(config);
        } catch (IOException e) {
            Log.e(TAG, "setXmlConfiguration: ", e);
        }
    }

    /**
     * Configuration the log4j system
     *
     * @param config the config
     */
    public static void setConfiguration(Configuration config) {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.start(config);
    }

    private static void injectPlugins(String packageName, Class<?>[] classes) {
        PluginRegistry reg = PluginRegistry.getInstance();
        try {
            Field f = reg.getClass().getDeclaredField("pluginsByCategoryByPackage");
            f.setAccessible(true);
            try {
                @SuppressWarnings("unchecked")
                ConcurrentMap<String, Map<String, List<PluginType<?>>>> map = (ConcurrentMap<String, Map<String, List<PluginType<?>>>>) f.get(reg);
                map.put(packageName, loadFromClasses(classes));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        PluginManager.addPackage(packageName);
    }

    private static Map<String, List<PluginType<?>>> loadFromClasses(Class<?>[] classes) {

        final ResolverUtil resolver = new ResolverUtil();
        final ClassLoader classLoader = Loader.getClassLoader();
        if (classLoader != null) {
            resolver.setClassLoader(classLoader);
        }
        final Map<String, List<PluginType<?>>> newPluginsByCategory = new HashMap<String, List<PluginType<?>>>();
        for (final Class<?> clazz : classes) {
            final Plugin plugin = clazz.getAnnotation(Plugin.class);
            final String categoryLowerCase = plugin.category().toLowerCase();
            List<PluginType<?>> list = newPluginsByCategory.get(categoryLowerCase);
            if (list == null) {
                newPluginsByCategory.put(categoryLowerCase, list = new ArrayList<>());
            }
            final PluginEntry mainEntry = new PluginEntry();
            final String mainElementName = plugin.elementType().equals(
                    Plugin.EMPTY) ? plugin.name() : plugin.elementType();
            mainEntry.setKey(plugin.name().toLowerCase());
            mainEntry.setName(plugin.name());
            mainEntry.setCategory(plugin.category());
            mainEntry.setClassName(clazz.getName());
            mainEntry.setPrintable(plugin.printObject());
            mainEntry.setDefer(plugin.deferChildren());
            @SuppressWarnings({"unchecked", "rawtypes"})
            final PluginType<?> mainType = new PluginType(mainEntry, clazz, mainElementName);
            list.add(mainType);
            final PluginAliases pluginAliases = clazz.getAnnotation(PluginAliases.class);
            if (pluginAliases != null) {
                for (final String alias : pluginAliases.value()) {
                    final PluginEntry aliasEntry = new PluginEntry();
                    final String aliasElementName = plugin.elementType().equals(
                            Plugin.EMPTY) ? alias.trim() : plugin.elementType();
                    aliasEntry.setKey(alias.trim().toLowerCase());
                    aliasEntry.setName(plugin.name());
                    aliasEntry.setCategory(plugin.category());
                    aliasEntry.setClassName(clazz.getName());
                    aliasEntry.setPrintable(plugin.printObject());
                    aliasEntry.setDefer(plugin.deferChildren());
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    final PluginType<?> aliasType = new PluginType(aliasEntry, clazz, aliasElementName);
                    list.add(aliasType);
                }
            }
        }
        return newPluginsByCategory;
    }
}
