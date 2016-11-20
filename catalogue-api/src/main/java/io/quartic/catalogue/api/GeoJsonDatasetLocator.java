package io.quartic.catalogue.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = GeoJsonDatasetLocatorImpl.class)
@JsonDeserialize(as = GeoJsonDatasetLocatorImpl.class)
public interface GeoJsonDatasetLocator extends DatasetLocator {
    String url();
}
