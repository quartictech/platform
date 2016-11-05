package io.quartic.terminator.api;

import io.quartic.catalogue.api.DatasetId;
import io.quartic.geojson.FeatureCollection;
import io.quartic.weyl.common.SweetStyle;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractFeatureCollectionWithDatasetId {
    DatasetId datasetId();
    FeatureCollection featureCollection();
}
