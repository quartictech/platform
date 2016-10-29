package io.quartic.weyl.core.importer;

import io.quartic.weyl.core.live.EnrichedFeedEvent;
import io.quartic.weyl.core.model.Feature;

import java.util.Collection;

public interface ImporterSubscriber {
    void accept(Collection<Feature> newFeatures, Collection<EnrichedFeedEvent> newFeedEvents);
}
