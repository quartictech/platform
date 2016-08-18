package io.quartic.weyl.core;

import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import io.quartic.weyl.core.connect.PostgisConnector;
import io.quartic.weyl.core.model.*;
import org.skife.jdbi.v2.DBI;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class LayerStore {
    private Map<String, Layer> rawLayers;
    private Map<String, IndexedLayer> indexedLayers;
    private DBI dbi;

    public LayerStore(DBI dbi) {
        this.dbi = dbi;
        this.rawLayers = Maps.newConcurrentMap();
        this.indexedLayers = Maps.newConcurrentMap();
    }

    public Optional<IndexedLayer> importPostgis(String name, String sql) {
        Optional<IndexedLayer> layer = new PostgisConnector(dbi).fetch(name, sql)
                .map(LayerStore::index);

        layer.ifPresent(indexedLayer -> {
            rawLayers.put(indexedLayer.layer().name(), indexedLayer.layer());
            indexedLayers.put(indexedLayer.layer().name(), indexedLayer);
        });

        return layer;
    }

     private static IndexedLayer index(RawLayer layer) {
        Collection<IndexedFeature> features = layer.features()
                .stream()
                .map(feature -> ImmutableIndexedFeature.builder()
                        .feature(feature)
                        .preparedGeometry(PreparedGeometryFactory.prepare(feature.geometry()))
                        .build())
                .collect(Collectors.toList());

        return ImmutableIndexedLayer.builder()
                .layer(layer)
                .spatialIndex(spatialIndex(layer.features()))
                .indexedFeatures(features)
                .build();
    }

    private static SpatialIndex spatialIndex(Collection<Feature> features) {
        STRtree stRtree = new STRtree();
        features.forEach(feature -> stRtree.insert(feature.geometry().getEnvelopeInternal(), feature));
        return stRtree;
    }
}
