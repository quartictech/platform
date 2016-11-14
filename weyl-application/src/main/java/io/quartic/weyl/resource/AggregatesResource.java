package io.quartic.weyl.resource;

import io.quartic.weyl.core.compute.AbstractHistogram;
import io.quartic.weyl.core.compute.HistogramCalculator;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

@Path("/aggregates")
@Value.Immutable
public abstract class AggregatesResource {
    private static final Logger LOG = LoggerFactory.getLogger(AggregatesResource.class);

    public static AggregatesResource of(FeatureStore featureStore) {
        return ImmutableAggregatesResource.of(featureStore);
    }

    @Value.Parameter
    protected abstract FeatureStore featureStore();
    @Value.Default
    protected HistogramCalculator calculator() {
        return new HistogramCalculator();
    }

    @POST
    @Path("/histograms")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<AbstractHistogram> getHistogram(List<FeatureId> featureIds) {
        LOG.info("Histogramming {} features", featureIds.size());

        final List<Entry<FeatureId, Feature>> features = map(featureIds, id -> new AbstractMap.SimpleEntry<>(id, featureStore().get(id)));
        throwIfAnyMissing(features);

        return calculator().calculate(map(features, Entry::getValue));
    }

    private void throwIfAnyMissing(List<Entry<FeatureId, Feature>> features) {
        final List<FeatureId> missingIds = features.stream()
                .filter(e -> e.getValue() == null)
                .map(Entry::getKey)
                .collect(toList());

        if (!missingIds.isEmpty()) {
            final List<String> rawIds = map(missingIds, FeatureId::uid);
            final String message = String.format("Histogram could not find featureIds: %s", rawIds);
            LOG.error(message);
            throw new NotFoundException(message);
        }
    }

    private <T, U> List<U> map(Collection<T> items, Function<T, U> func) {
        return items.stream().map(func).collect(toList());
    }
}
