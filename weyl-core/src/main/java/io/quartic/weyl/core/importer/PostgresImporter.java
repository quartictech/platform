package io.quartic.weyl.core.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import io.quartic.jester.api.PostgresDatasetSource;
import io.quartic.weyl.core.attributes.ComplexAttribute;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.ImmutableFeature;
import org.postgresql.util.PGobject;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.ResultIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.IOException;
import java.util.*;

import static java.util.Collections.emptyList;

public class PostgresImporter implements Importer {
    private static final Logger log = LoggerFactory.getLogger(PostgresImporter.class);
    private static final String GEOM_WKB_FIELD = "geom_wkb";
    private static final String GEOM_FIELD = "geom";
    private static final String ID_FIELD = "id";
    private static final Set<String> RESERVED_KEYS = ImmutableSet.of(GEOM_FIELD, GEOM_WKB_FIELD, ID_FIELD);

    private final FeatureStore featureStore;
    private final WKBReader wkbReader = new WKBReader();
    private final ObjectMapper objectMapper;
    private final DBI dbi;
    private final String query;

    public static PostgresImporter create(PostgresDatasetSource source, FeatureStore featureStore, ObjectMapper objectMapper) {
        return new PostgresImporter(new DBI(source.url(), source.user(), source.password()), source.query(), featureStore, objectMapper);
    }

    public PostgresImporter(DBI dbi, String query, FeatureStore featureStore, ObjectMapper objectMapper) {
        this.dbi = dbi;
        this.query = query;
        this.featureStore = featureStore;
        this.objectMapper = objectMapper;
    }

    @Override
    public Observable<SourceUpdate> getObservable() {
        return Observable.create(sub -> {
            sub.onNext(SourceUpdate.of(importAllFeatures(), emptyList()));
            sub.onCompleted();
        });
    }

    private Collection<Feature> importAllFeatures() {
        try (final Handle h = dbi.open()) {
            final String expandedQuery = String.format("SELECT ST_AsBinary(ST_Transform(geom, 900913)) as geom_wkb, * FROM (%s) as data WHERE geom IS NOT NULL",
                    query);
            final ResultIterator<Map<String, Object>> iterator = h.createQuery(expandedQuery).iterator();

            Collection<Feature> features = Lists.newArrayList();
            int count = 0;
            while (iterator.hasNext()) {
                count += 1;
                if (count % 10000 == 0) {
                    log.info("Importing feature: {}", count);
                }
                Optional<Feature> feature = rowToFeature(iterator.next());

                feature.ifPresent(features::add);
            }
            iterator.close();
            return features;
        }
    }

    private Optional<Feature> rowToFeature(Map<String, Object> row) {
        byte[] wkb = (byte[]) row.get(GEOM_WKB_FIELD);

        if (wkb == null) {
            log.error("Missing required geometry field: " + GEOM_WKB_FIELD);
            return Optional.empty();
        }

        return parseGeometry(wkb).map(geometry -> {
            Map<String, Object> attributes = Maps.newHashMap();

            for(Map.Entry<String, Object> entry : row.entrySet()) {
                if (!RESERVED_KEYS.contains(entry.getKey()) && entry.getValue() != null) {
                    if (entry.getValue() instanceof PGobject) {
                        readPgObject(entry.getValue())
                                .ifPresent(attribute -> attributes.put(entry.getKey(), attribute));
                    }
                    else {
                        attributes.put(entry.getKey(), entry.getValue());
                    }
                }
            }

            String id = row.containsKey(ID_FIELD) ?
                    row.get(ID_FIELD).toString() : String.valueOf(geometry.hashCode());

            return ImmutableFeature.builder()
                    .geometry(geometry)
                    .metadata(attributes)
                    .uid(featureStore.getFeatureIdGenerator().get())
                    .externalId(id)
                    .build();
        });
    }

    private Optional<Geometry> parseGeometry(byte[] data) {
        try {
            return Optional.of(wkbReader.read(data));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    private Optional<Object> readPgObject(Object value) {
        PGobject pgObject = (PGobject) value;
        if (pgObject.getType().equals("json") || pgObject.getType().equals("jsonb")) {
            try {
                return Optional.of(objectMapper.readValue(pgObject.getValue(), ComplexAttribute.class));
            } catch (IOException e) {
                log.warn("exception parsing json to attribute: {}", e.toString());
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
