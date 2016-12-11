package io.quartic.weyl.core.source;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import io.quartic.catalogue.api.PostgresDatasetLocator;
import io.quartic.weyl.core.LayerUpdate;
import io.quartic.weyl.core.LayerUpdateImpl;
import io.quartic.weyl.core.attributes.AttributesFactory;
import io.quartic.weyl.core.attributes.ComplexAttribute;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.model.NakedFeatureImpl;
import org.immutables.value.Value;
import org.postgresql.util.PGobject;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.ResultIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static io.quartic.common.serdes.ObjectMappers.OBJECT_MAPPER;

@Value.Immutable
public abstract class PostgresSource implements Source {
    public static ImmutablePostgresSource.Builder builder() {
        return ImmutablePostgresSource.builder();
    }

    private static final Logger LOG = LoggerFactory.getLogger(PostgresSource.class);
    private static final String GEOM_WKB_FIELD = "geom_wkb";
    private static final String GEOM_FIELD = "geom";
    private static final String ID_FIELD = "id";
    private static final Set<String> RESERVED_KEYS = ImmutableSet.of(GEOM_FIELD, GEOM_WKB_FIELD, ID_FIELD);

    private final WKBReader wkbReader = new WKBReader();
    protected abstract String name();
    protected abstract PostgresDatasetLocator locator();
    protected abstract AttributesFactory attributesFactory();

    @Value.Default
    protected DBI dbi() {
        return new DBI(locator().url(), locator().user(), locator().password());
    }

    @Override
    public Observable<LayerUpdate> observable() {
        return Observable.create(sub -> {
            sub.onNext(LayerUpdateImpl.of(importAllFeatures()));
            sub.onCompleted();
        });
    }

    @Override
    public boolean indexable() {
        return true;
    }

    private Collection<NakedFeature> importAllFeatures() {
        try (final Handle h = dbi().open()) {
            LOG.info("[{}] Connection established", name());

            final String query = String.format("SELECT ST_AsBinary(ST_Transform(geom, 900913)) as geom_wkb, * FROM (%s) as data WHERE geom IS NOT NULL", locator().query());
            final ResultIterator<Map<String, Object>> iterator = h.createQuery(query).iterator();

            Collection<NakedFeature> features = Lists.newArrayList();
            int count = 0;
            while (iterator.hasNext()) {
                count += 1;
                if (count % 10000 == 0) {
                    LOG.info("[{}] Importing feature: {}", name(), count);
                }
                Optional<NakedFeature> feature = rowToFeature(iterator.next());

                feature.ifPresent(features::add);
            }
            iterator.close();
            return features;
        }
    }

    private Optional<NakedFeature> rowToFeature(Map<String, Object> row) {
        byte[] wkb = (byte[]) row.get(GEOM_WKB_FIELD);

        if (wkb == null) {
            LOG.error("[{}] Missing required geometry field: " + GEOM_WKB_FIELD, name());
            return Optional.empty();
        }

        return parseGeometry(wkb).map(geometry -> {
            final AttributesFactory.AttributesBuilder builder = attributesFactory().builder();

            for(Map.Entry<String, Object> entry : row.entrySet()) {
                if (!RESERVED_KEYS.contains(entry.getKey()) && entry.getValue() != null) {
                    if (entry.getValue() instanceof PGobject) {
                        readPgObject(entry.getValue())
                                .ifPresent(attribute -> builder.put(entry.getKey(), attribute));
                    }
                    else {
                        builder.put(entry.getKey(), entry.getValue());
                    }
                }
            }

            Optional<String> id = Optional.ofNullable((String) row.get(ID_FIELD));

            return NakedFeatureImpl.of(id, geometry, builder.build());
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
                return Optional.of(OBJECT_MAPPER.readValue(pgObject.getValue(), ComplexAttribute.class));
            } catch (IOException e) {
                LOG.warn("[{}] Exception parsing json to attribute: {}", name(), e.toString());
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
