package io.quartic.weyl.core;

import com.google.common.collect.ImmutableList;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import io.quartic.weyl.core.feature.FeatureCollection;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureImpl;
import io.quartic.weyl.core.model.ImmutableIndexedFeature;
import io.quartic.weyl.core.model.IndexedFeature;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerImpl;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.core.model.LayerStatsImpl;
import io.quartic.weyl.core.model.LayerUpdate;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.model.SnapshotImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import static io.quartic.weyl.core.StatsCalculator.calculateStats;
import static io.quartic.weyl.core.attributes.AttributeSchemaInferrer.inferSchema;
import static io.quartic.weyl.core.feature.FeatureCollection.EMPTY_COLLECTION;
import static io.quartic.weyl.core.model.DynamicSchema.EMPTY_SCHEMA;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

public class SnapshotReducer {
    private static final Logger LOG = LoggerFactory.getLogger(SnapshotReducer.class);
    private final AtomicInteger missingExternalIdGenerator = new AtomicInteger();

    public Snapshot empty(LayerSpec spec) {
        return SnapshotImpl.of(
                LayerImpl.builder()
                        .spec(spec)
                        .features(EMPTY_COLLECTION)
                        .dynamicSchema(EMPTY_SCHEMA)
                        .spatialIndex(new STRtree())
                        .indexedFeatures(ImmutableList.of())
                        .stats(LayerStatsImpl.of(emptyMap()))
                        .build(),
                emptyList()
        );
    }

    public Snapshot next(Snapshot previous, LayerUpdate update) {
        final Layer prevLayer = previous.absolute();

        LOG.info("[{}] Accepted {} features", prevLayer.spec().metadata().name(), update.features().size());

        final Collection<Feature> elaborated = elaborate(prevLayer.spec().id(), update.features());
        return SnapshotImpl.of(next(prevLayer, elaborated), elaborated);
    }

    private Layer next(Layer layer, Collection<Feature> features) {
        final FeatureCollection updatedFeatures = layer.features().append(features);
        final LayerImpl withFeatures = LayerImpl.copyOf(layer)
                .withFeatures(updatedFeatures)
                .withDynamicSchema(inferSchema(features, layer.dynamicSchema(), layer.spec().staticSchema()));

        if (layer.spec().indexable()) {
            final Collection<IndexedFeature> indexedFeatures = indexedFeatures(updatedFeatures);
            return withFeatures
                    .withSpatialIndex(spatialIndex(indexedFeatures))
                    .withIndexedFeatures(indexedFeatures)
                    .withStats(calculateStats(withFeatures.dynamicSchema(), updatedFeatures));
        } else {
            return withFeatures;
        }
    }

    private static Collection<IndexedFeature> indexedFeatures(FeatureCollection features) {
        return features.stream()
                .map(feature -> ImmutableIndexedFeature.builder()
                        .feature(feature)
                        .preparedGeometry(PreparedGeometryFactory.prepare(feature.geometry()))
                        .build())
                .collect(toList());
    }

    private static SpatialIndex spatialIndex(Collection<IndexedFeature> features) {
        STRtree stRtree = new STRtree();
        features.forEach(feature -> stRtree.insert(feature.preparedGeometry().getGeometry().getEnvelopeInternal(), feature));
        return stRtree;
    }


    private Collection<Feature> elaborate(LayerId layerId, Collection<NakedFeature> features) {
        return features.stream().map(f -> FeatureImpl.of(
                new EntityId(layerId.getUid() + "/" +
                        f.externalId().orElse(String.valueOf(missingExternalIdGenerator.incrementAndGet()))),
                f.geometry(),
                f.attributes()
        )).collect(toList());
    }
}
