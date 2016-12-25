package io.quartic.common.client;

import io.quartic.common.ApplicationDetails;

public final class Utils {
    private Utils() {}

    public static String userAgentFor(Class<?> clazz) {
        final ApplicationDetails details = new ApplicationDetails(clazz);
        return details.getName() + "/" + details.getVersion() + " (Java " + details.getJavaVersion() + ")";
    }
}
