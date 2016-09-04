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

public class BucketOp {
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

    public static Optional<RawLayer> create(LayerStore store, BucketSpec bucketSpec) {
        Optional<IndexedLayer> featureLayer = store.get(bucketSpec.features());
        Optional<IndexedLayer> bucketLayer = store.get(bucketSpec.buckets());

        if (featureLayer.isPresent() && bucketLayer.isPresent()) {
            SpatialIndex bucketIndex = bucketLayer.get().spatialIndex();

            ForkJoinPool forkJoinPool = new ForkJoinPool(4);
            try {
                Collection<Feature> features = forkJoinPool.submit(() -> bucketData(featureLayer.get(), bucketSpec, bucketIndex)).get();
                String layerName = String.format("%s (bucketed)",
                        featureLayer.get().layer().metadata().name());
                String layerDescription = String.format("%s bucketed by %s aggregating by %s",
                        featureLayer.get().layer().metadata().name(),
                        bucketLayer.get().layer().metadata().name(),
                        bucketSpec.aggregation().toString());


                RawLayer layer = ImmutableRawLayer.builder()
                        .features(features)
                        .metadata(ImmutableLayerMetadata.builder()
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

        return Optional.empty();
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
                    Double value = aggregation.aggregate(feature, bucketEntry.getValue().stream().map(Bucketed::getValue).collect(Collectors.toList()));
                    Map<String, Optional<Object>> metadata = new HashMap<>(feature.metadata());
                    metadata.put(propertyName, Optional.of(value));
                    return ImmutableFeature.copyOf(feature)
                            .withMetadata(metadata);
                })
                .collect(Collectors.toList());
    }
}
