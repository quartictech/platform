package io.quartic.weyl.core.compute;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.SpatialIndex;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.model.*;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

public class BucketOp {
    private final FeatureStore featureStore;
    private final AbstractIndexedLayer featureLayer;
    private final BucketSpec bucketSpec;
    private final AbstractIndexedLayer bucketLayer;

    private static class Bucketed {
        private final Feature bucket;
        private final Feature value;

        private Bucketed(Feature bucket, Feature value) {
            this.bucket = bucket;
            this.value = value;
        }

        Feature getBucket() {
            return bucket;
        }

        public Feature getValue() {
            return value;
        }
    }

    private BucketOp(FeatureStore featureStore, AbstractIndexedLayer featureLayer, AbstractIndexedLayer bucketLayer, BucketSpec bucketSpec) {
        this.featureStore = featureStore;
        this.featureLayer = featureLayer;
        this.bucketLayer = bucketLayer;
        this.bucketSpec = bucketSpec;
    }

    private String propertyName() {
        return featureLayer.layer().metadata().name();
    }

    public static Optional<Layer> create(LayerStore store, BucketSpec bucketSpec) {
        Optional<IndexedLayer> featureLayer = store.getLayer(bucketSpec.features());
        Optional<IndexedLayer> bucketLayer = store.getLayer(bucketSpec.buckets());

        if (featureLayer.isPresent() && bucketLayer.isPresent()) {
            return new BucketOp(store.getFeatureStore(), featureLayer.get(), bucketLayer.get(), bucketSpec).compute();
        }

        return Optional.empty();
    }

    Optional<Layer> compute() {
        ForkJoinPool forkJoinPool = new ForkJoinPool(4);
        try {
            Collection<Feature> features = forkJoinPool.submit(this::bucketData).get();
            String layerName = String.format("%s (bucketed)",
                    featureLayer.layer().metadata().name());
            String layerDescription = String.format("%s bucketed by %s aggregating by %s",
                    featureLayer.layer().metadata().name(),
                    bucketLayer.layer().metadata().name(),
                    bucketSpec.aggregation().toString());

            Map<String, AbstractAttribute> attributeMap = Maps.newHashMap(bucketLayer.layer()
                    .schema().attributes());
            AbstractAttribute newAttribute = Attribute.builder()
                    .type(AttributeType.NUMERIC)
                    .build();
            attributeMap.put(propertyName(), newAttribute);

            AttributeSchema attributeSchema = ImmutableAttributeSchema
                    .copyOf(bucketLayer.layer().schema())
                    .withAttributes(attributeMap)
                    .withPrimaryAttribute(propertyName());

            Layer layer = Layer.builder()
                    .features(featureStore.newCollection().append(features))
                    .schema(attributeSchema)
                    .metadata(LayerMetadata.builder()
                            .name(layerName)
                            .description(layerDescription)
                            .build())
                    .build();
            return Optional.of(layer);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private Collection<Feature> bucketData() {
        SpatialIndex bucketIndex = bucketLayer.spatialIndex();
        List<Bucketed> hits = featureLayer.layer().features().parallelStream()
                .flatMap(feature -> {
                    Geometry featureGeometry = feature.geometry();
                    List<IndexedFeature> buckets = bucketIndex.query(featureGeometry.getEnvelopeInternal());

                    return buckets.stream()
                            .filter(hitFeature -> hitFeature.preparedGeometry().contains(featureGeometry))
                            .map(hitFeature -> new Bucketed(hitFeature.feature(), feature));
                })
                .collect(Collectors.toList());

        Multimap<Feature, Bucketed> groups = Multimaps.index(hits, Bucketed::getBucket);

        BucketAggregation aggregation = bucketSpec.aggregation();

        return groups.asMap().entrySet().parallelStream()
                .map(bucketEntry -> {
                    Feature feature = bucketEntry.getKey();
                    Double value = aggregation.aggregate(
                            feature,
                            bucketEntry.getValue().stream().map(Bucketed::getValue).collect(Collectors.toList()));

                    if (bucketSpec.normalizeToArea()) {
                        if (feature.geometry().getArea() > 0) {
                            value /= feature.geometry().getArea();
                        }
                    }
                    Map<String, Object> metadata = new HashMap<>(feature.metadata());
                    metadata.put(propertyName(), value);
                    return ImmutableFeature.copyOf(feature)
                            .withUid(featureStore.getFeatureIdGenerator().get())
                            .withMetadata(metadata);
                })
                .collect(Collectors.toList());
    }
}
