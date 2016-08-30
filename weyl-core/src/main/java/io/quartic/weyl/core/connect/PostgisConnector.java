package io.quartic.weyl.core.connect;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import io.quartic.weyl.core.model.*;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class PostgisConnector {
    private static final Logger log = LoggerFactory.getLogger(PostgisConnector.class);
    private static final String GEOM_WKB_FIELD = "geom_wkb";
    private static final String GEOM_FIELD = "geom";
    private static final String ID_FIELD = "id";
    private static final Set<String> RESERVED_KEYS = ImmutableSet.of(GEOM_FIELD, GEOM_WKB_FIELD, ID_FIELD);

    private final DBI dbi;
    private final WKBReader wkbReader;

    public PostgisConnector(DBI dbi) {
        this.dbi = dbi;
        wkbReader = new WKBReader();
    }

    public Optional<RawLayer> fetch(LayerMetadata metadata, String sql) {
        Handle h = dbi.open();
        String sqlExpanded = String.format("SELECT ST_AsBinary(geom) as geom_wkb, * FROM (%s) as data",
                sql);
        List<Optional<Feature>> optionalFeatures = h.createQuery(sqlExpanded)
                .list()
                .stream()
                .map(this::rowToFeature)
                .collect(Collectors.toList());

        if (! optionalFeatures.stream().allMatch(Optional::isPresent)) {
            log.error("Error parsing features from query result");
            return Optional.empty();
        }

        List<Feature> features = optionalFeatures.stream()
                .map(Optional::get)
                .collect(Collectors.toList());

        return Optional.of(ImmutableRawLayer.builder()
                .features(features)
                .metadata(metadata)
                .build());
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

        if (wkb == null) {
            log.error("Missing required geometry field: " + GEOM_WKB_FIELD);
            return Optional.empty();
        }

        return parseGeomtry(wkb).map( geometry -> {
            Map<String, Optional<Object>> attributes = Maps.newHashMap();

            for(Map.Entry<String, Object> entry : row.entrySet()) {
                if (! RESERVED_KEYS.contains(entry.getKey())) {
                    attributes.put(entry.getKey(), Optional.ofNullable(entry.getValue()));
                }
            }

            String id = (String) row.get(ID_FIELD);

            if (id == null) {
                id = String.valueOf(geometry.hashCode());
            }

            return ImmutableFeature.builder()
                    .geometry(geometry)
                    .metadata(attributes)
                    .id(id)
                    .build();
        });
    }
}
