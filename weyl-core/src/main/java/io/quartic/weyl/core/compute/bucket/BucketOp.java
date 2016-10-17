package io.quartic.weyl.core.compute.bucket;

import com.google.common.collect.Maps;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.compute.SpatialJoin;
import io.quartic.weyl.core.compute.Tuple;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.model.*;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

public class BucketOp {
    private final FeatureStore featureStore;
    private final AbstractLayer featureLayer;
    private final BucketSpec bucketSpec;
    private final AbstractLayer bucketLayer;

    private BucketOp(FeatureStore featureStore, AbstractLayer featureLayer, AbstractLayer bucketLayer, BucketSpec bucketSpec) {
        this.featureStore = featureStore;
        this.featureLayer = featureLayer;
        this.bucketLayer = bucketLayer;
        this.bucketSpec = bucketSpec;
    }

    private String propertyName() {
        return featureLayer.metadata().name();
    }

    public static Optional<BucketResults> create(LayerStore store, BucketSpec bucketSpec) {
        Optional<Layer> featureLayer = store.getLayer(bucketSpec.features());
        Optional<Layer> bucketLayer = store.getLayer(bucketSpec.buckets());

        if (featureLayer.isPresent() && bucketLayer.isPresent()) {
            return new BucketOp(store.getFeatureStore(), featureLayer.get(), bucketLayer.get(), bucketSpec).compute();
        }

        return Optional.empty();
    }

    Optional<BucketResults> compute() {
        ForkJoinPool forkJoinPool = new ForkJoinPool(4);
        try {
            Collection<Feature> features = forkJoinPool.submit(this::bucketData).get();
            String layerName = String.format("%s (bucketed)",
                    featureLayer.metadata().name());
            String layerDescription = String.format("%s bucketed by %s aggregating by %s",
                    featureLayer.metadata().name(),
                    bucketLayer.metadata().name(),
                    bucketSpec.aggregation().toString());

            Map<String, AbstractAttribute> attributeMap = Maps.newHashMap(bucketLayer.schema().attributes());
            AbstractAttribute newAttribute = Attribute.builder()
                    .type(AttributeType.NUMERIC)
                    .build();
            attributeMap.put(propertyName(), newAttribute);

            AttributeSchema schema = ImmutableAttributeSchema
                    .copyOf(bucketLayer.schema())
                    .withAttributes(attributeMap)
                    .withPrimaryAttribute(propertyName());

            return Optional.of(BucketResults.of(
                    LayerMetadata.builder()
                            .name(layerName)
                            .description(layerDescription)
                            .build(),
                    features,
                    schema
            ));
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private Collection<Feature> bucketData() {
        // The order here is that CONTAINS applies from left -> right and
        // the spatial index on the right layer is the one that is queried
        Map<Feature, List<Tuple>> groups = SpatialJoin.innerJoin(bucketLayer, featureLayer,
                SpatialJoin.SpatialPredicate.CONTAINS)
                .collect(Collectors.groupingBy(Tuple::left));

        BucketAggregation aggregation = bucketSpec.aggregation();

        return groups.entrySet().parallelStream()
                .map(bucketEntry -> {
                    Feature bucket = bucketEntry.getKey();
                    Double value = aggregation.aggregate(
                            bucket,
                            bucketEntry.getValue().stream().map(Tuple::right).collect(Collectors.toList()));

                    if (bucketSpec.normalizeToArea()) {
                        if (bucket.geometry().getArea() > 0) {
                            value /= bucket.geometry().getArea();
                        }
                    }
                    Map<String, Object> metadata = new HashMap<>(bucket.metadata());
                    metadata.put(propertyName(), value);
                    return ImmutableFeature.copyOf(bucket)
                            .withUid(featureStore.getFeatureIdGenerator().get())
                            .withMetadata(metadata);
                })
                .collect(Collectors.toList());
    }
}
