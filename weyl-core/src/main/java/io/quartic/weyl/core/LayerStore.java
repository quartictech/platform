package io.quartic.weyl.core;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import io.quartic.weyl.core.compute.BucketLayer;
import io.quartic.weyl.core.compute.BucketSpec;
import io.quartic.weyl.core.connect.PostgisConnector;
import io.quartic.weyl.core.model.*;
import org.skife.jdbi.v2.DBI;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class LayerStore {
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

        layer.ifPresent(indexedLayer -> {
            rawLayers.put(indexedLayer.layerId(), indexedLayer.layer());
            indexedLayers.put(indexedLayer.layerId(), indexedLayer);
        });

        return layer;
    }

    public Collection<IndexedLayer> listLayers() {
        return indexedLayers.values();
    }

    public Optional<IndexedLayer> get(LayerId layerId) {
       return Optional.of(indexedLayers.get(layerId));
    }

    public Optional<IndexedLayer> bucket(BucketSpec bucketSpec) {
        return BucketLayer.create(this, bucketSpec)
                .map(LayerStore::index);
    }

     private static IndexedLayer index(Layer layer) {
         Collection<IndexedFeature> features = layer.features()
                .stream()
                .map(feature -> ImmutableIndexedFeature.builder()
                        .feature(feature)
                        .preparedGeometry(PreparedGeometryFactory.prepare(feature.geometry()))
                        .build())
                .collect(Collectors.toList());

         LayerId layerId = ImmutableLayerId.builder().id(UUID.randomUUID().toString()).build();
         return ImmutableIndexedLayer.builder()
                .layer(layer)
                .spatialIndex(spatialIndex(features))
                .indexedFeatures(features)
                .layerId(layerId)
                .build();
    }

    private static SpatialIndex spatialIndex(Collection<IndexedFeature> features) {
        STRtree stRtree = new STRtree();
        features.forEach(feature -> stRtree.insert(feature.preparedGeometry().getGeometry().getEnvelopeInternal(), feature));
        return stRtree;
    }
}
