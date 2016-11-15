package io.quartic.common;

import io.quartic.weyl.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public abstract class AbstractApplicationDetails {
    protected abstract Class<?> clazz();

    @Value.Lazy
    public String name() {
        final String name = clazz().getSimpleName();
        return name.replaceAll("Application$", "");
    }

    @Value.Lazy
    public String version() {
        final String version = clazz().getPackage().getImplementationVersion();
        return ((version == null) ? "unknown" : version);
    }

    @Value.Lazy
    public String javaVersion() {
        return System.getProperty("java.version");
    }
}
