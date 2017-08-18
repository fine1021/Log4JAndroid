package android.support.log4j2.lookup;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.core.lookup.AbstractLookup;
import org.apache.logging.log4j.core.lookup.ContextMapLookup;
import org.apache.logging.log4j.core.lookup.DateLookup;
import org.apache.logging.log4j.core.lookup.EnvironmentLookup;
import org.apache.logging.log4j.core.lookup.JavaLookup;
import org.apache.logging.log4j.core.lookup.MapLookup;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.apache.logging.log4j.core.lookup.SystemPropertiesLookup;
import org.apache.logging.log4j.core.util.ReflectionUtil;
import org.apache.logging.log4j.status.StatusLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Proxies all the other {@link StrLookup}s.
 */

public class AndroidInterpolator extends AbstractLookup {

    private static final Logger LOGGER = StatusLogger.getLogger();

    /**
     * Constant for the prefix separator.
     */
    private static final char PREFIX_SEPARATOR = ':';

    private final Map<String, StrLookup> lookups = new HashMap<>();

    private final StrLookup defaultLookup;

    public AndroidInterpolator(final StrLookup defaultLookup) {
        this(defaultLookup, null);
    }

    /**
     * Constructs an Interpolator using a given StrLookup and a list of packages to find Lookup plugins in.
     *
     * @param defaultLookup  the default StrLookup to use as a fallback
     * @param pluginPackages a list of packages to scan for Lookup plugins
     * @since 2.1
     */
    public AndroidInterpolator(final StrLookup defaultLookup, final List<String> pluginPackages) {
        this.defaultLookup = defaultLookup == null ? new MapLookup(new HashMap<String, String>()) : defaultLookup;
        final PluginManager manager = new PluginManager(CATEGORY);
        manager.collectPlugins(pluginPackages);
        final Map<String, PluginType<?>> plugins = manager.getPlugins();

        for (final Map.Entry<String, PluginType<?>> entry : plugins.entrySet()) {
            try {
                final Class<? extends StrLookup> clazz = entry.getValue().getPluginClass().asSubclass(StrLookup.class);
                lookups.put(entry.getKey(), ReflectionUtil.instantiate(clazz));
            } catch (final Exception ex) {
                LOGGER.error("Unable to create Lookup for {}", entry.getKey(), ex);
            }
        }
    }

    /**
     * Create the default Interpolator using only Lookups that work without an event.
     */
    public AndroidInterpolator() {
        this((Map<String, String>) null);
    }

    /**
     * Creates the Interpolator using only Lookups that work without an event and initial properties.
     */
    public AndroidInterpolator(final Map<String, String> properties) {
        this.defaultLookup = new MapLookup(properties == null ? new HashMap<String, String>() : properties);
        // TODO: this ought to use the PluginManager
        lookups.put("sys", new SystemPropertiesLookup());
        lookups.put("env", new EnvironmentLookup());
        lookups.put("android", new AndroidLookup());
        lookups.put("java", new JavaLookup());
        lookups.put("date", new DateLookup());
        lookups.put("ctx", new ContextMapLookup());
    }

    /**
     * Resolves the specified variable. This implementation will try to extract
     * a variable prefix from the given variable name (the first colon (':') is
     * used as prefix separator). It then passes the name of the variable with
     * the prefix stripped to the lookup object registered for this prefix. If
     * no prefix can be found or if the associated lookup object cannot resolve
     * this variable, the default lookup object will be used.
     *
     * @param event The current LogEvent or null.
     * @param var   the name of the variable whose value is to be looked up
     * @return the value of this variable or <b>null</b> if it cannot be
     * resolved
     */
    @Override
    public String lookup(final LogEvent event, String var) {
        if (var == null) {
            return null;
        }

        final int prefixPos = var.indexOf(PREFIX_SEPARATOR);
        if (prefixPos >= 0) {
            final String prefix = var.substring(0, prefixPos);
            final String name = var.substring(prefixPos + 1);
            final StrLookup lookup = lookups.get(prefix);
            String value = null;
            if (lookup != null) {
                value = event == null ? lookup.lookup(name) : lookup.lookup(event, name);
            }

            if (value != null) {
                return value;
            }
            var = var.substring(prefixPos + 1);
        }
        if (defaultLookup != null) {
            return event == null ? defaultLookup.lookup(var) : defaultLookup.lookup(event, var);
        }
        return null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (final String name : lookups.keySet()) {
            if (sb.length() == 0) {
                sb.append('{');
            } else {
                sb.append(", ");
            }

            sb.append(name);
        }
        if (sb.length() > 0) {
            sb.append('}');
        }
        return sb.toString();
    }
}

