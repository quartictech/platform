package io.quartic.weyl.resource;

import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.FeatureId;
import org.slf4j.Logger;

import javax.ws.rs.NotFoundException;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

public class FeatureStoreQuerier {
    private static final Logger LOG = getLogger(FeatureStoreQuerier.class);

    private final FeatureStore featureStore;

    public FeatureStoreQuerier(FeatureStore featureStore) {
        this.featureStore = featureStore;
    }

    public Stream<AbstractFeature> retrieveFeaturesOrThrow(List<FeatureId> featureIds) {
        LOG.info("Retrieving {} features", featureIds.size());

        final List<Entry<FeatureId, AbstractFeature>> features = map(featureIds, id -> new AbstractMap.SimpleEntry<>(id, featureStore.get(id)));
        throwIfAnyMissing(features);

        return features.stream().map(Entry::getValue);
    }

    private void throwIfAnyMissing(List<Entry<FeatureId, AbstractFeature>> features) {
        final List<FeatureId> missingIds = features.stream()
                .filter(e -> e.getValue() == null)
                .map(Entry::getKey)
                .collect(toList());

        if (!missingIds.isEmpty()) {
            final List<String> rawIds = map(missingIds, FeatureId::uid);
            final String message = String.format("Could not retrieve featureIds: %s", rawIds);
            LOG.error(message);
            throw new NotFoundException(message);
        }
    }

    private <T, U> List<U> map(Collection<T> items, Function<T, U> func) {
        return items.stream().map(func).collect(toList());
    }
}
