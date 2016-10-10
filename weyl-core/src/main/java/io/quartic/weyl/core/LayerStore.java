package io.quartic.weyl.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import io.quartic.weyl.core.attributes.AttributeSchemaInferrer;
import io.quartic.weyl.core.compute.BucketOp;
import io.quartic.weyl.core.compute.BucketSpec;
import io.quartic.weyl.core.importer.Importer;
import io.quartic.weyl.core.importer.PostgresImporter;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.model.*;
import io.quartic.weyl.core.utils.UidGenerator;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class LayerStore {
    private static final Logger log = LoggerFactory.getLogger(LayerStore.class);
    private final Map<LayerId, Layer> rawLayers = Maps.newConcurrentMap();
    private final Map<LayerId, IndexedLayer> indexedLayers = Maps.newConcurrentMap();
    private final FeatureStore featureStore;
    private final UidGenerator<LayerId> lidGenerator;
    private final ObjectMapper objectMapper;

    public LayerStore(FeatureStore featureStore, UidGenerator<LayerId> lidGenerator, ObjectMapper objectMapper) {
        this.featureStore = featureStore;
        this.lidGenerator = lidGenerator;
        this.objectMapper = objectMapper;
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

        RawLayer rawLayer = ImmutableRawLayer.builder()
                .features(featureStore.createImmutableCollection(features))
                .metadata(metadata)
                .schema(attributeSchema)
                .build();

        IndexedLayer layer = index(rawLayer);
        storeLayer(layer);

        return layer;
    }

    public FeatureStore getFeatureStore() {
        return featureStore;
    }

    private void storeLayer(IndexedLayer indexedLayer) {
        rawLayers.put(indexedLayer.layerId(), indexedLayer.layer());
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

     private IndexedLayer index(Layer layer) {
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

    private static LayerStats calculateStats(Layer layer) {
        Map<String, Double> maxNumeric = Maps.newConcurrentMap();
        Map<String, Double> minNumeric = Maps.newConcurrentMap();

        AttributeSchema attributeSchema = layer.schema();

        layer.features().parallelStream()
                .flatMap(feature -> feature.metadata().entrySet().stream())
                .filter(entry -> attributeSchema.attributes().get(entry.getKey()).type()
                        == AttributeType.NUMERIC)
                .forEach(entry -> {
                            Object value = entry.getValue();
                            double doubleValue = Double.valueOf(value.toString());

                            if (!maxNumeric.containsKey(entry.getKey())) {
                                maxNumeric.put(entry.getKey(), doubleValue);
                            } else if (doubleValue > maxNumeric.get(entry.getKey())) {
                                maxNumeric.put(entry.getKey(), doubleValue);
                            }
                            if (!minNumeric.containsKey(entry.getKey())) {
                                minNumeric.put(entry.getKey(), doubleValue);
                            } else if (doubleValue < minNumeric.get(entry.getKey())) {
                                minNumeric.put(entry.getKey(), doubleValue);
                            }
                        });

        ImmutableLayerStats.Builder builder = ImmutableLayerStats.builder();
        layer.schema().attributes()
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().type() == AttributeType.NUMERIC)
                .forEach(entry -> builder.putAttributeStats(entry.getKey(),
                        ImmutableAttributeStats.builder()
                                .minimum(minNumeric.get(entry.getKey()))
                                .maximum(maxNumeric.get(entry.getKey()))
                        .build()));

        builder.featureCount(layer.features().size());

        return builder.build();
    }

    private static SpatialIndex spatialIndex(Collection<IndexedFeature> features) {
        STRtree stRtree = new STRtree();
        features.forEach(feature -> stRtree.insert(feature.preparedGeometry().getGeometry().getEnvelopeInternal(), feature));
        return stRtree;
    }
}
