package io.quartic.terminator.api;

import io.quartic.catalogue.api.TerminationId;
import io.quartic.common.SweetStyle;
import io.quartic.geojson.FeatureCollection;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractFeatureCollectionWithTerminationId {
    TerminationId terminationId();
    FeatureCollection featureCollection();
}
