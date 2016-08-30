package io.quartic.weyl.core.compute;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.SpatialIndex;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.model.*;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

public class BucketLayer implements Layer {
    private final Collection<Feature> features;
    private final LayerMetadata metadata;

    private static class Bucketed<T> {
        private final Feature bucket;
        private final T value;

        private Bucketed(Feature bucket, T value) {
            this.bucket = bucket;
            this.value = value;
        }

        Feature getBucket() {
            return bucket;
        }

        public T getValue() {
            return value;
        }
    }

    public static Optional<BucketLayer> create(LayerStore store, BucketSpec bucketSpec) {
        Optional<IndexedLayer> featureLayer = store.get(bucketSpec.features());
        Optional<IndexedLayer> bucketLayer = store.get(bucketSpec.buckets());

        if (featureLayer.isPresent() && bucketLayer.isPresent()) {
            SpatialIndex bucketIndex = bucketLayer.get().spatialIndex();

            ForkJoinPool forkJoinPool = new ForkJoinPool(4);
            try {
                Collection<Feature> features = forkJoinPool.submit(() -> bucketData(featureLayer.get(), bucketSpec, bucketIndex)).get();
                return Optional.of(new BucketLayer(features, featureLayer.get().layer().metadata().name(), bucketLayer.get().layer().metadata().name(), bucketSpec.layerName()));
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    private BucketLayer(Collection<Feature> features, String bucketLayerName, String featureLayerName, String newLayerName) {
        this.features = features;
        this.metadata = ImmutableLayerMetadata.builder()
                .name(newLayerName)
                .description(String.format("Layer %s bucketed by %s", featureLayerName, bucketLayerName))
                .build();
    }

    private static Collection<Feature> bucketData(IndexedLayer featureLayer, BucketSpec bucketSpec, SpatialIndex bucketIndex) {
        List<Bucketed<Feature>> hits = featureLayer.layer().features().parallelStream()
                .flatMap(feature -> {
                    Geometry featureGeometry = feature.geometry();
                    List<IndexedFeature> buckets = bucketIndex.query(featureGeometry.getEnvelopeInternal());

                    return buckets.stream().filter(hitFeature -> hitFeature.preparedGeometry().contains(featureGeometry))
                            .map(hitFeature -> new Bucketed<>(hitFeature.feature(), feature));
                })
                .collect(Collectors.toList());

        Multimap<Feature, Bucketed<Feature>> groups = Multimaps
                .index(hits, Bucketed::getBucket);

        BucketAggregation aggregation = bucketSpec.aggregation();
        String propertyName = bucketSpec.aggregationPropertyName();

        return groups.asMap().entrySet().parallelStream()
                .map(bucketEntry -> {
                    Feature feature = bucketEntry.getKey();
                    Double value = aggregation.aggregate(bucketEntry.getValue().stream().map(Bucketed::getValue).collect(Collectors.toList()));
                    Map<String, Optional<Object>> metadata = new HashMap<>(feature.metadata());
                    metadata.put(propertyName, Optional.of(value));
                    return ImmutableFeature.copyOf(feature)
                            .withMetadata(metadata);
                })
                .collect(Collectors.toList());
    }


    @Override
    public LayerMetadata metadata() {
        return metadata;
    }

    @Override
    public Collection<Feature> features() {
        return features;
    }
}
