package io.quartic.weyl.core.source;

import io.quartic.weyl.common.SweetStyle;
import io.quartic.weyl.core.live.EnrichedFeedEvent;
import io.quartic.weyl.core.model.Feature;
import org.immutables.value.Value;

import java.util.Collection;

@SweetStyle
@Value.Immutable
public interface AbstractSourceUpdate {
    // TODO: not Collections
    Collection<Feature> features();
    Collection<EnrichedFeedEvent> feedEvents();
}
