package io.quartic.weyl.core.compute;

import com.google.common.collect.Maps;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.attributes.AttributesFactory;
import io.quartic.weyl.core.compute.SpatialJoin.Tuple;
import io.quartic.weyl.core.model.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

public class BucketComputation implements LayerComputation {
    private final Layer featureLayer;
    private final BucketSpec bucketSpec;
    private final Layer bucketLayer;
    private final AttributesFactory attributesFactory = new AttributesFactory();

    private BucketComputation(Layer featureLayer, Layer bucketLayer, BucketSpec bucketSpec) {
        this.featureLayer = featureLayer;
        this.bucketLayer = bucketLayer;
        this.bucketSpec = bucketSpec;
    }

    public static BucketComputation create(LayerStore store, BucketSpec bucketSpec) {
        Optional<Layer> featureLayer = store.getLayer(bucketSpec.features());
        Optional<Layer> bucketLayer = store.getLayer(bucketSpec.buckets());

        if (featureLayer.isPresent() && bucketLayer.isPresent()) {
            return new BucketComputation(featureLayer.get(), bucketLayer.get(), bucketSpec);
        }
        else {
            throw new RuntimeException("can't find input layers for bucket computation");
        }
    }

    @Override
    public Optional<ComputationResults> compute() {
        ForkJoinPool forkJoinPool = new ForkJoinPool(4);
        try {
            Collection<Feature> features = forkJoinPool.submit(this::bucketData).get();
            String layerName = String.format("%s (bucketed)",
                    rawAttributeName());
            String layerDescription = String.format("%s bucketed by %s aggregating by %s",
                    rawAttributeName(),
                    bucketLayer.metadata().name(),
                    bucketSpec.aggregation().toString());

            Map<AttributeName, Attribute> attributeMap = Maps.newHashMap(bucketLayer.schema().attributes());
            Attribute newAttribute = AttributeImpl.builder()
                    .type(AttributeType.NUMERIC)
                    .build();
            attributeMap.put(attributeName(), newAttribute);

            AttributeSchema schema = AttributeSchemaImpl
                    .copyOf(bucketLayer.schema())
                    .withAttributes(attributeMap)
                    .withPrimaryAttribute(attributeName());

            return Optional.of(ComputationResultsImpl.of(
                    LayerMetadataImpl.builder()
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

                    final AttributesFactory.AttributesBuilder builder = attributesFactory.builder(bucket.attributes());
                    builder.put(rawAttributeName(), value);
                    return FeatureImpl.copyOf(bucket).withAttributes(builder.build());
                })
                .collect(Collectors.toList());
    }

    private AttributeName attributeName() {
        return AttributeNameImpl.of(rawAttributeName());
    }

    private String rawAttributeName() {
        return featureLayer.metadata().name();
    }
}
