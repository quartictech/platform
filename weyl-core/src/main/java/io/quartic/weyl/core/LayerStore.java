package io.quartic.weyl.core;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import io.quartic.weyl.core.attributes.AttributeSchemaInferrer;
import io.quartic.weyl.core.compute.BucketOp;
import io.quartic.weyl.core.compute.BucketSpec;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.importer.Importer;
import io.quartic.weyl.core.model.*;
import io.quartic.weyl.core.utils.UidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.quartic.weyl.core.StatsCalculator.calculateStats;

public class LayerStore {
    private static final Logger log = LoggerFactory.getLogger(LayerStore.class);
    private final Map<LayerId, IndexedLayer> indexedLayers = Maps.newConcurrentMap();
    private final FeatureStore featureStore;
    private final UidGenerator<LayerId> lidGenerator;

    public LayerStore(FeatureStore featureStore, UidGenerator<LayerId> lidGenerator) {
        this.featureStore = featureStore;
        this.lidGenerator = lidGenerator;
    }

    public IndexedLayer importLayer(Importer importer, LayerMetadata metadata) {
        Collection<Feature> features = importer.get();
        log.info("imported {} features", features.size());
        log.info("envelope: {}:", Iterables.getFirst(features, null).geometry().getEnvelopeInternal());
        Map<String, AbstractAttribute> attributes = AttributeSchemaInferrer.inferSchema(features);

        AttributeSchema attributeSchema = ImmutableAttributeSchema.builder()
                .attributes(attributes)
                .primaryAttribute(Optional.empty())
                .build();

        AbstractLayer layer = Layer.builder()
                .metadata(metadata)
                .features(featureStore.newCollection().append(importer.get()))
                .schema(attributeSchema)
                .build();

        IndexedLayer indexedLayer = index(layer);
        storeLayer(indexedLayer);

        return indexedLayer;
    }

    public FeatureStore getFeatureStore() {
        return featureStore;
    }

    private void storeLayer(IndexedLayer indexedLayer) {
        indexedLayers.put(indexedLayer.layerId(), indexedLayer);
    }

    public Collection<IndexedLayer> listLayers() {
        return indexedLayers.values();
    }

    public Optional<IndexedLayer> get(LayerId layerId) {
       return Optional.ofNullable(indexedLayers.get(layerId));
    }

    public Optional<IndexedLayer> bucket(BucketSpec bucketSpec) {
         Optional<IndexedLayer> layer = BucketOp.create(this, bucketSpec)
                .map(this::index);

        layer.ifPresent(this::storeLayer);
        return layer;
    }

     private IndexedLayer index(AbstractLayer layer) {
         Collection<IndexedFeature> features = layer.features()
                .stream()
                .map(feature -> ImmutableIndexedFeature.builder()
                        .feature(feature)
                        .preparedGeometry(PreparedGeometryFactory.prepare(feature.geometry()))
                        .build())
                .collect(Collectors.toList());

         return ImmutableIndexedLayer.builder()
                 .layer(layer)
                 .spatialIndex(spatialIndex(features))
                 .indexedFeatures(features)
                 .layerId(lidGenerator.get())
                 .layerStats(calculateStats(layer))
                 .build();
    }

    private static SpatialIndex spatialIndex(Collection<IndexedFeature> features) {
        STRtree stRtree = new STRtree();
        features.forEach(feature -> stRtree.insert(feature.preparedGeometry().getGeometry().getEnvelopeInternal(), feature));
        return stRtree;
    }
}
