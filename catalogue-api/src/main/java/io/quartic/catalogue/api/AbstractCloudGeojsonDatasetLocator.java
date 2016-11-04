package io.quartic.catalogue.api;

import io.quartic.weyl.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractCloudGeojsonDatasetLocator extends DatasetLocator {
    String path();
}
