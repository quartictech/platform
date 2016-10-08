package io.quartic.weyl.core.connect;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import io.quartic.weyl.core.attributes.AttributeSchemaInferrer;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.model.*;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.ResultIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class PostgisConnector {
    private static final Logger log = LoggerFactory.getLogger(PostgisConnector.class);
    private static final String GEOM_WKB_FIELD = "geom_wkb";
    private static final String GEOM_FIELD = "geom";
    private static final String ID_FIELD = "id";
    private static final Set<String> RESERVED_KEYS = ImmutableSet.of(GEOM_FIELD, GEOM_WKB_FIELD, ID_FIELD);

    private final FeatureStore featureStore;
    private final DBI dbi;
    private final WKBReader wkbReader = new WKBReader();

    public PostgisConnector(FeatureStore featureStore, DBI dbi) {
        this.featureStore = featureStore;
        this.dbi = dbi;
    }

    public Optional<RawLayer> fetch(LayerMetadata metadata, String sql) {
        Handle h = dbi.open();
        String sqlExpanded = String.format("SELECT ST_AsBinary(ST_Transform(geom, 900913)) as geom_wkb, * FROM (%s) as data WHERE geom IS NOT NULL",
                sql);
        ResultIterator<Map<String, Object>> iterator = h.createQuery(sqlExpanded)
                .iterator();

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

        Map<String, AbstractAttribute> attributes = AttributeSchemaInferrer.inferSchema(features);

        AttributeSchema attributeSchema = ImmutableAttributeSchema.builder()
                .attributes(attributes)
                .primaryAttribute(Optional.empty())
                .build();

        return Optional.of(ImmutableRawLayer.builder()
                .features(featureStore.createImmutableCollection(features))
                .metadata(metadata)
                .schema(attributeSchema)
                .build());
    }



    private Optional<Geometry> parseGeometry(byte[] data) {
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

        return parseGeometry(wkb).map(geometry -> {
            Map<String, Object> attributes = Maps.newHashMap();

            for(Map.Entry<String, Object> entry : row.entrySet()) {
                if (!RESERVED_KEYS.contains(entry.getKey()) && entry.getValue() != null) {
                    attributes.put(entry.getKey(), entry.getValue());
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
}
