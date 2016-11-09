package io.quartic.common.client;

public final class Utils {
    private Utils() {}

    public static String userAgentFor(Class<?> clazz) {
        final String version = clazz.getPackage().getImplementationVersion();
        return strippedName(clazz)
                + "/" + ((version == null) ? "unknown" : version)
                + " (Java " + System.getProperty("java.version") + ")";
    }

    private static String strippedName(Class<?> clazz) {
        final String name = clazz.getSimpleName();
        return name.replaceAll("Application$", "");
    }
}
