package io.quartic.weyl.response;

import io.quartic.weyl.core.feed.AbstractElaboratedFeedEvent;
import io.quartic.weyl.core.feed.SequenceId;
import io.quartic.weyl.core.geojson.SweetStyle;
import org.immutables.value.Value;

import java.util.List;

@SweetStyle
@Value.Immutable
public interface AbstractFeedResponse {
    List<AbstractElaboratedFeedEvent> events();
    SequenceId nextSequenceId();
}
