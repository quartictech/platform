package io.quartic.weyl.core.compute;

import io.quartic.common.SweetStyle;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.attributes.AttributesFactory;
import io.quartic.weyl.core.compute.SpatialJoiner.Tuple;
import io.quartic.weyl.core.model.*;
import org.immutables.value.Value;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import static com.google.common.collect.Maps.newHashMap;
import static io.quartic.weyl.core.compute.SpatialJoiner.SpatialPredicate.CONTAINS;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@SweetStyle
@Value.Immutable
public abstract class BucketComputation implements LayerComputation {
    protected abstract LayerStore store();
    protected abstract BucketSpec bucketSpec();

    @Value.Derived
    protected Layer featureLayer() {
        return store().getLayer(bucketSpec().features()).get();
    }

    @Value.Derived
    protected Layer bucketLayer() {
        return store().getLayer(bucketSpec().buckets()).get();
    }

    @Value.Default
    protected SpatialJoiner joiner() {
        return new SpatialJoiner();
    }

    private final AttributesFactory attributesFactory = new AttributesFactory();

    @Override
    public Optional<ComputationResults> compute() {
        ForkJoinPool forkJoinPool = new ForkJoinPool(4);
        try {
            return results(forkJoinPool.submit(this::bucketData).get(), schema());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private Optional<ComputationResults> results(Collection<Feature> features, AttributeSchema schema) {
        String layerName = String.format("%s (bucketed)",
                rawAttributeName());
        String layerDescription = String.format("%s bucketed by %s aggregating by %s",
                rawAttributeName(),
                bucketLayer().metadata().name(),
                bucketSpec().aggregation().toString());
        return Optional.of(ComputationResultsImpl.of(
                LayerMetadataImpl.builder()
                        .name(layerName)
                        .description(layerDescription)
                        .build(),
                features,
                schema
        ));
    }

    private AttributeSchema schema() {
        Map<AttributeName, Attribute> attributeMap = newHashMap(bucketLayer().schema().attributes());
        Attribute newAttribute = AttributeImpl.builder()
                .type(AttributeType.NUMERIC)
                .build();
        attributeMap.put(attributeName(), newAttribute);

        return AttributeSchemaImpl
                .copyOf(bucketLayer().schema())
                .withAttributes(attributeMap)
                .withPrimaryAttribute(attributeName());
    }

    private Collection<Feature> bucketData() {
        // The order here is that CONTAINS applies from left -> right and
        // the spatial index on the right layer is the one that is queried
        Map<Feature, List<Tuple>> groups = joiner().innerJoin(bucketLayer(), featureLayer(), CONTAINS)
                .collect(groupingBy(Tuple::left));

        return groups.entrySet().parallelStream()
                .map(this::featureForBucket)
                .collect(toList());
    }

    private Feature featureForBucket(Entry<Feature, List<Tuple>> entry) {
        Feature bucket = entry.getKey();
        Double value = bucketSpec().aggregation().aggregate(
                bucket,
                entry.getValue().stream().map(Tuple::right).collect(toList()));

        if (bucketSpec().normalizeToArea()) {
            if (bucket.geometry().getArea() > 0) {
                value /= bucket.geometry().getArea();
            }
        }

        final AttributesFactory.AttributesBuilder builder = attributesFactory.builder(bucket.attributes());
        builder.put(rawAttributeName(), value);
        return FeatureImpl.copyOf(bucket).withAttributes(builder.build());
    }

    private AttributeName attributeName() {
        return AttributeNameImpl.of(rawAttributeName());
    }

    private String rawAttributeName() {
        return featureLayer().metadata().name();
    }
}
