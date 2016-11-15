package io.quartic.catalogue.api;

import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractPostgresDatasetLocator extends DatasetLocator {
    String user();
    String password();
    String url();
    String query();
}
