package io.quartic.weyl.core.feed;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.quartic.weyl.core.geojson.SweetStyle;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

@SweetStyle
@Value.Immutable
public interface AbstractElaboratedFeedEvent {
    @JsonUnwrapped
    AbstractFeedEvent event();
    LayerId layerId();
    FeatureId featureId();
    FeedIcon icon();
}
