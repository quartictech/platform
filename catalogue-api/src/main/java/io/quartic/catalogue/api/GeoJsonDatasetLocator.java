package io.quartic.catalogue.api;

import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface GeoJsonDatasetLocator extends DatasetLocator {
    String url();
}
