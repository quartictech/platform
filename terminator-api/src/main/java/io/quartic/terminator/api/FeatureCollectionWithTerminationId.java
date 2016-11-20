package io.quartic.terminator.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.catalogue.api.TerminationId;
import io.quartic.common.SweetStyle;
import io.quartic.geojson.FeatureCollection;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = FeatureCollectionWithTerminationIdImpl.class)
@JsonDeserialize(as = FeatureCollectionWithTerminationIdImpl.class)
public interface FeatureCollectionWithTerminationId {
    TerminationId terminationId();
    FeatureCollection featureCollection();
}
