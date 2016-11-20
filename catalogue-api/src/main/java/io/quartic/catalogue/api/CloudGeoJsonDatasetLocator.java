package io.quartic.catalogue.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = CloudGeoJsonDatasetLocatorImpl.class)
@JsonDeserialize(as = CloudGeoJsonDatasetLocatorImpl.class)
public interface CloudGeoJsonDatasetLocator extends DatasetLocator {
    String path();
}
