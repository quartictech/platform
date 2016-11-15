package io.quartic.weyl.message;

import io.quartic.common.SweetStyle;
import io.quartic.geojson.FeatureCollection;
import io.quartic.weyl.core.live.EnrichedFeedEvent;
import io.quartic.weyl.core.model.AttributeSchema;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.LayerId;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;

@SweetStyle
@Value.Immutable
public interface AbstractLayerUpdateMessage extends SocketMessage {
    LayerId layerId();
    AttributeSchema schema();
    FeatureCollection featureCollection();
    List<EnrichedFeedEvent> feedEvents();
    // TODO: This is a hack for live charts/selection
    Map<String, FeatureId> externalIdToFeatureIdMapping();
}
