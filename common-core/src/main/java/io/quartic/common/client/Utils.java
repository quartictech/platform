package io.quartic.common.client;

import io.quartic.common.ApplicationDetails;
import io.quartic.common.ApplicationDetailsImpl;

public final class Utils {
    private Utils() {}

    public static String userAgentFor(Class<?> clazz) {
        final ApplicationDetails details = ApplicationDetailsImpl.of(clazz);
        return details.name() + "/" + details.version() + " (Java " + details.javaVersion() + ")";
    }
}
