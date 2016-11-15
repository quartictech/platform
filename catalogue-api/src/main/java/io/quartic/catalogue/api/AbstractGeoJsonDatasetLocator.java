package io.quartic.catalogue.api;

import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractGeoJsonDatasetLocator extends DatasetLocator {
    String url();
}
