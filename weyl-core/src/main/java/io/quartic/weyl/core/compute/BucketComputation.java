package io.quartic.weyl.core.compute;

import com.google.common.collect.Maps;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.model.*;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

public class BucketComputation implements LayerComputation {
    private final FeatureStore featureStore;
    private final AbstractLayer featureLayer;
    private final AbstractBucketSpec bucketSpec;
    private final AbstractLayer bucketLayer;

    private BucketComputation(FeatureStore featureStore, AbstractLayer featureLayer, AbstractLayer bucketLayer, AbstractBucketSpec bucketSpec) {
        this.featureStore = featureStore;
        this.featureLayer = featureLayer;
        this.bucketLayer = bucketLayer;
        this.bucketSpec = bucketSpec;
    }

    private AttributeName attributeName() {
        return AttributeName.of(featureLayer.metadata().name());
    }

    public static BucketComputation create(LayerStore store, AbstractBucketSpec bucketSpec) {
        Optional<AbstractLayer> featureLayer = store.getLayer(bucketSpec.features());
        Optional<AbstractLayer> bucketLayer = store.getLayer(bucketSpec.buckets());

        if (featureLayer.isPresent() && bucketLayer.isPresent()) {
            return new BucketComputation(store.getFeatureStore(), featureLayer.get(), bucketLayer.get(), bucketSpec);
        }
        else {
            throw new RuntimeException("can't find input layers for bucket computation");
        }
    }

    @Override
    public Optional<ComputationResults> compute() {
        ForkJoinPool forkJoinPool = new ForkJoinPool(4);
        try {
            Collection<AbstractFeature> features = forkJoinPool.submit(this::bucketData).get();
            String layerName = String.format("%s (bucketed)",
                    featureLayer.metadata().name());
            String layerDescription = String.format("%s bucketed by %s aggregating by %s",
                    featureLayer.metadata().name(),
                    bucketLayer.metadata().name(),
                    bucketSpec.aggregation().toString());

            Map<AttributeName, AbstractAttribute> attributeMap = Maps.newHashMap(bucketLayer.schema().attributes());
            AbstractAttribute newAttribute = Attribute.builder()
                    .type(AttributeType.NUMERIC)
                    .build();
            attributeMap.put(attributeName(), newAttribute);

            AttributeSchema schema = AttributeSchema
                    .copyOf(bucketLayer.schema())
                    .withAttributes(attributeMap)
                    .withPrimaryAttribute(attributeName());

            return Optional.of(ComputationResults.of(
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

    private Collection<AbstractFeature> bucketData() {
        // The order here is that CONTAINS applies from left -> right and
        // the spatial index on the right layer is the one that is queried
        Map<AbstractFeature, List<Tuple>> groups = SpatialJoin.innerJoin(bucketLayer, featureLayer,
                SpatialJoin.SpatialPredicate.CONTAINS)
                .collect(Collectors.groupingBy(Tuple::left));

        BucketAggregation aggregation = bucketSpec.aggregation();

        return groups.entrySet().parallelStream()
                .map(bucketEntry -> {
                    AbstractFeature bucket = bucketEntry.getKey();
                    Double value = aggregation.aggregate(
                            bucket,
                            bucketEntry.getValue().stream().map(Tuple::right).collect(Collectors.toList()));

                    if (bucketSpec.normalizeToArea()) {
                        if (bucket.geometry().getArea() > 0) {
                            value /= bucket.geometry().getArea();
                        }
                    }

                    final Attributes.Builder builder = Attributes.builder();
                    builder.attributes(bucket.attributes().attributes());
                    builder.attribute(attributeName(), value);
                    return Feature.copyOf(bucket)
                            .withUid(featureStore.getFeatureIdGenerator().get())
                            .withAttributes(builder.build());
                })
                .collect(Collectors.toList());
    }
}
