package io.quartic.weyl.core.source;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.live.EnrichedFeedEvent;
import io.quartic.weyl.core.model.AbstractNakedFeature;
import org.immutables.value.Value;

import java.util.Collection;

@SweetStyle
@Value.Immutable
public interface AbstractSourceUpdate {
    // TODO: not Collections
    Collection<AbstractNakedFeature> features();
    Collection<EnrichedFeedEvent> feedEvents();
}
