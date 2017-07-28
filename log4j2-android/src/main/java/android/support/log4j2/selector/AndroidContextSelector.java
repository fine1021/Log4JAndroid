package android.support.log4j2.selector;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.selector.ContextSelector;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@code ContextSelector} that returns the singleton, used by Android
 */

public class AndroidContextSelector implements ContextSelector {

    private static final LoggerContext CONTEXT = new LoggerContext("AndroidContextSelector@"
            + AndroidContextSelector.class.hashCode());

    @Override
    public LoggerContext getContext(String fqcn, ClassLoader loader, boolean currentContext) {
        return CONTEXT;
    }

    @Override
    public LoggerContext getContext(String fqcn, ClassLoader loader, boolean currentContext, URI configLocation) {
        return CONTEXT;
    }

    @Override
    public List<LoggerContext> getLoggerContexts() {
        final List<LoggerContext> list = new ArrayList<>();
        list.add(CONTEXT);
        return Collections.unmodifiableList(list);
    }

    @Override
    public void removeContext(LoggerContext context) {

    }
}
