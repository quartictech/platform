package io.quartic.weyl.core.connect;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.ImmutableFeature;
import io.quartic.weyl.core.model.ImmutableLayer;
import io.quartic.weyl.core.model.Layer;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class PostgisConnector {
    private static final String GEOM_WKB_FIELD = "geom_wkb";
    private static final String GEOM_FIELD = "geom";
    private static final String ID_FIELD = "id";

    private final DBI dbi;
    private final WKBReader wkbReader;

    public PostgisConnector(DBI dbi) {
        this.dbi = dbi;
        wkbReader = new WKBReader();
    }

    public Layer fetch(String name, String sql) throws IOException {
        Handle h = dbi.open();
        List<Optional<Feature>> optionalFeatures = h.createQuery(sql)
                .list()
                .stream()
                .map(this::rowToFeature)
                .collect(Collectors.toList());

        if (! optionalFeatures.stream().allMatch(Optional::isPresent)) {
            throw new IOException("Error parsing features from query result");
        }

        List<Feature> features = optionalFeatures.stream()
                .map(Optional::get)
                .collect(Collectors.toList());

        return ImmutableLayer.builder()
                .features(features)
                .index(spatialIndex(features))
                .name(name)
                .build();
    }

    private SpatialIndex spatialIndex(List<Feature> features) {
        STRtree stRtree = new STRtree();
        features.forEach(feature -> stRtree.insert(feature.geometry().getGeometry().getEnvelopeInternal(), feature));
        return stRtree;
    }

    private Optional<Geometry> parseGeomtry(byte[] data) {
        try {
            return Optional.of(wkbReader.read(data));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    private Optional<Feature> rowToFeature(Map<String, Object> row) {
        byte[] wkb = (byte[]) row.get(GEOM_WKB_FIELD);

        return parseGeomtry(wkb).map( geometry -> {
            Map<String, Object> attributes = new HashMap<>(row);
            attributes.remove(GEOM_FIELD);
            attributes.remove(GEOM_WKB_FIELD);
            attributes.remove(ID_FIELD);

            String id = (String) row.get(ID_FIELD);

            PreparedGeometry preparedGeometry = PreparedGeometryFactory.prepare(geometry);
            return ImmutableFeature.builder()
                    .geometry(preparedGeometry)
                    .id(id)
                    .build();
        });
    }
}
