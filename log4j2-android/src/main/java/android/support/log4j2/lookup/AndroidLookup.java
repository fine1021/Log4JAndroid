package android.support.log4j2.lookup;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.StrLookup;

import java.util.HashMap;
import java.util.Map;

/**
 * AndroidLookup
 */

@Plugin(name = "android", category = StrLookup.CATEGORY)
public class AndroidLookup implements StrLookup {

    private static final Map<String, String> LOOK_UP_MAP = new HashMap<>();

    public static final String FILES_DIR = "files.dir";
    public static final String EXTERNAL_FILES_DIR = "external.files.dir";
    public static final String EXTERNAL_LOGS_DIR = "external.logs.dir";
    public static final String EXTERNAL_STORAGE_DIR = "external.storage.dir";

    @Override
    public String lookup(String key) {
        return LOOK_UP_MAP.get(key);
    }

    @Override
    public String lookup(LogEvent event, String key) {
        return lookup(key);
    }

    public static Map<String, String> getLookUpMap() {
        return LOOK_UP_MAP;
    }
}
