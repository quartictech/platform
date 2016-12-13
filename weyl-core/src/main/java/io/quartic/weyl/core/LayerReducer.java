package io.quartic.weyl.core;

import com.google.common.collect.ImmutableList;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import io.quartic.weyl.core.feature.FeatureCollection;
import io.quartic.weyl.core.model.AttributeSchemaImpl;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.ImmutableIndexedFeature;
import io.quartic.weyl.core.model.IndexedFeature;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerImpl;
import io.quartic.weyl.core.model.LayerSpec;
import io.quartic.weyl.core.model.LayerSpecImpl;
import io.quartic.weyl.core.model.LayerStatsImpl;

import java.util.Collection;

import static io.quartic.weyl.core.StatsCalculator.calculateStats;
import static io.quartic.weyl.core.attributes.AttributeSchemaInferrer.inferSchema;
import static io.quartic.weyl.core.feature.FeatureCollection.EMPTY_COLLECTION;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

public class LayerReducer {

    public Layer create(LayerSpec spec) {
        return LayerImpl.builder()
                .spec(spec)
                .features(EMPTY_COLLECTION)
                .spatialIndex(new STRtree())
                .indexedFeatures(ImmutableList.of())
                .stats(LayerStatsImpl.of(emptyMap(), 0))
                .build();
    }

    public Layer reduce(Layer layer, Collection<Feature> features) {
        final FeatureCollection updatedFeatures = layer.features().append(features);
        final LayerImpl withFeatures = LayerImpl.copyOf(layer)
                .withFeatures(updatedFeatures)
                .withSpec(LayerSpecImpl.copyOf(layer.spec())
                        .withSchema(AttributeSchemaImpl.copyOf(layer.spec().schema())
                                .withAttributes(inferSchema(features, layer.spec().schema().attributes()))
                        )
                );

        if (layer.spec().indexable()) {
            final Collection<IndexedFeature> indexedFeatures = indexedFeatures(updatedFeatures);
            return withFeatures
                    .withSpatialIndex(spatialIndex(indexedFeatures))
                    .withIndexedFeatures(indexedFeatures)
                    .withStats(calculateStats(withFeatures.spec().schema(), updatedFeatures));
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
}
