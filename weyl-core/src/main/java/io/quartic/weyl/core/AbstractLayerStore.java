package io.quartic.weyl.core;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.live.LiveLayerView;
import io.quartic.weyl.core.model.*;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.quartic.weyl.core.StatsCalculator.calculateStats;
import static io.quartic.weyl.core.attributes.AttributeSchemaInferrer.inferSchema;

public abstract class AbstractLayerStore {
    protected final FeatureStore featureStore;
    protected final Map<LayerId, IndexedLayer> layers = Maps.newConcurrentMap();

    public AbstractLayerStore(FeatureStore featureStore) {
        this.featureStore = featureStore;
    }

    public void createLayer(LayerId id, LayerMetadata metadata, LiveLayerView view) {
        if (layers.containsKey(id)) {
            final IndexedLayer old = layers.get(id);
            putLayer(old
                    .withLayer(old.layer().withMetadata(metadata))
                    .withView(view)
            );
        } else {
            io.quartic.weyl.core.feature.FeatureCollection features = featureStore.newCollection();

            Layer layer = Layer.builder()
                    .metadata(metadata)
                    .schema(createSchema(features))
                    .features(features)
                    .build();

            putLayer(index(id, layer, view));
        }
    }

    public Collection<IndexedLayer> listLayers() {
        return layers.entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    protected ImmutableAttributeSchema createSchema(io.quartic.weyl.core.feature.FeatureCollection features) {
        return ImmutableAttributeSchema.builder()
                .attributes(inferSchema(features))
                .primaryAttribute(Optional.empty())
                .build();
    }

    protected void checkLayerExists(LayerId layerId) {
        Preconditions.checkArgument(layers.containsKey(layerId), "No layer with id=" + layerId.uid());
    }

    protected void putLayer(IndexedLayer layer) {
        layers.put(layer.layerId(), layer);
    }

    protected IndexedLayer index(LayerId layerId, Layer layer, LiveLayerView view) {
        Collection<IndexedFeature> features = layer.features()
                .stream()
                .map(feature -> ImmutableIndexedFeature.builder()
                        .feature(feature)
                        .preparedGeometry(PreparedGeometryFactory.prepare(feature.geometry()))
                        .build())
                .collect(Collectors.toList());

        return IndexedLayer.builder()
                .layer(layer)
                .spatialIndex(spatialIndex(features))
                .indexedFeatures(features)
                .layerId(layerId)
                .layerStats(calculateStats(layer))
                .feedEvents(ImmutableList.of())     // TODO
                .view(view)
                .build();
    }

    private static SpatialIndex spatialIndex(Collection<IndexedFeature> features) {
        STRtree stRtree = new STRtree();
        features.forEach(feature -> stRtree.insert(feature.preparedGeometry().getGeometry().getEnvelopeInternal(), feature));
        return stRtree;
    }

    public Optional<IndexedLayer> get(LayerId layerId) {
       return Optional.ofNullable(layers.get(layerId));
    }

    public FeatureStore getFeatureStore() {
        return featureStore;
    }
}
