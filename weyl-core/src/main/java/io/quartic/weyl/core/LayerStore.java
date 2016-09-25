package io.quartic.weyl.core;

import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import io.quartic.weyl.core.compute.BucketOp;
import io.quartic.weyl.core.compute.BucketSpec;
import io.quartic.weyl.core.connect.PostgisConnector;
import io.quartic.weyl.core.model.*;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class LayerStore {
    private static final Logger log = LoggerFactory.getLogger(LayerStore.class);
    private Map<LayerId, Layer> rawLayers;
    private Map<LayerId, IndexedLayer> indexedLayers;
    private DBI dbi;

    public LayerStore(DBI dbi) {
        this.dbi = dbi;
        this.rawLayers = Maps.newConcurrentMap();
        this.indexedLayers = Maps.newConcurrentMap();
    }

    public Optional<IndexedLayer> importPostgis(LayerMetadata metadata, String sql) {
        Optional<IndexedLayer> layer = new PostgisConnector(dbi).fetch(metadata, sql)
                .map(LayerStore::index);

        layer.ifPresent(this::storeLayer);

        return layer;
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
                .map(LayerStore::index);

        layer.ifPresent(this::storeLayer);
        return layer;
    }

     private static IndexedLayer index(Layer layer) {
         Collection<IndexedFeature> features = layer.features()
                .stream()
                .map(feature -> ImmutableIndexedFeature.builder()
                        .feature(feature)
                        .preparedGeometry(PreparedGeometryFactory.prepare(feature.geometry()))
                        .build())
                .collect(Collectors.toList());

         LayerId layerId = LayerId.of(UUID.randomUUID().toString());
         return ImmutableIndexedLayer.builder()
                 .layer(layer)
                 .spatialIndex(spatialIndex(features))
                 .indexedFeatures(features)
                 .layerId(layerId)
                 .layerStats(calculateStats(layer))
                 .build();
    }

    private static LayerStats calculateStats(Layer layer) {
        Map<String, Double> maxNumeric = Maps.newConcurrentMap();
        Map<String, Double> minNumeric = Maps.newConcurrentMap();

        AttributeSchema attributeSchema = layer.schema();

        layer.features().parallelStream()
                .flatMap(feature -> feature.metadata().entrySet().stream())
                .filter(entry -> entry.getValue().isPresent())
                .filter(entry -> attributeSchema.attributes().get(entry.getKey()).type()
                        == AttributeType.NUMERIC)
                .forEach(entry -> {
                            Object value = entry.getValue().get();
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
