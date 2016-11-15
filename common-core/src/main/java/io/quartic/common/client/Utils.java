package io.quartic.common.client;

import io.quartic.common.ApplicationDetails;

public final class Utils {
    private Utils() {}

    public static String userAgentFor(Class<?> clazz) {
        final ApplicationDetails details = ApplicationDetails.of(clazz);
        return details.name() + "/" + details.version() + " (Java " + details.javaVersion() + ")";
    }
}
