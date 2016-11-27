package io.quartic.weyl.core.feature;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quartic.weyl.core.attributes.AttributesFactory;
import io.quartic.weyl.core.attributes.ComplexAttribute;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.core.model.Attributes;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.model.NakedFeatureImpl;
import io.quartic.weyl.core.utils.GeometryTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.AbstractMap.SimpleImmutableEntry;

import static com.google.common.collect.Sets.filter;
import static io.quartic.common.serdes.ObjectMappers.OBJECT_MAPPER;
import static io.quartic.weyl.core.attributes.AttributeUtils.isSimple;
import static io.quartic.weyl.core.utils.GeometryTransformer.wgs84toWebMercator;
import static java.util.stream.Collectors.toList;

public class FeatureConverter {
    private static final Logger LOG = LoggerFactory.getLogger(FeatureConverter.class);

    private final GeometryTransformer geometryTransformer;
    private final AttributesFactory attributesFactory;

    public FeatureConverter(AttributesFactory attributesFactory) {
        this(attributesFactory, wgs84toWebMercator());
    }

    public FeatureConverter(AttributesFactory attributesFactory, GeometryTransformer geometryTransformer) {
        this.attributesFactory = attributesFactory;
        this.geometryTransformer = geometryTransformer;
    }

    public Collection<NakedFeature> toModel(io.quartic.geojson.FeatureCollection featureCollection) {
        return featureCollection.features().stream()
                .filter(f -> f.geometry().isPresent())
                .map(this::toModel)
                .collect(toList());
    }

    private NakedFeature toModel(io.quartic.geojson.Feature f) {
        // HACK: we can assume that we've simply filtered out features with null geometries for now
        return NakedFeatureImpl.of(
                f.id(),
                geometryTransformer.transform(Utils.toJts(f.geometry().get())),
                convertToModelAttributes(f.properties())
        );
    }

    private Attributes convertToModelAttributes(Map<String, Object> rawAttributes) {
        final AttributesFactory.AttributesBuilder builder = attributesFactory.builder();
        rawAttributes.forEach((k, v) -> builder.put(k, convertAttributeValue(OBJECT_MAPPER, k, v)));
        return builder.build();
    }

    private static Object convertAttributeValue(ObjectMapper objectMapper, String key, Object value) {
        // TODO: Move this up into generic code behind the importers
        if (value instanceof Map) {
            try {
                return objectMapper.convertValue(value, ComplexAttribute.class);
            }
            catch (IllegalArgumentException e) {
                LOG.warn("Couldn't convert attribute {}. Exception: {}", key, e);
                return value;
            }
        }
        return value;
    }

    /**
     * This is really only efficient for iteration over entrySet().  Everything else will be slow.
     */
    public static Map<String, Object> getRawProperties(Feature feature) {
        final Set<Map.Entry<String, Object>> filtered = filter(
                feature.attributes().attributes().entrySet(),
                e -> isSimple(e.getValue())
        );

        final SimpleImmutableEntry<String, Object> idEntry = new SimpleImmutableEntry<>("_entityId", feature.entityId().uid());

        return new AbstractMap<String, Object>() {
            @Override
            public Set<Entry<String, Object>> entrySet() {
                return new AbstractSet<Entry<String, Object>>() {
                    @Override
                    public Iterator<Entry<String, Object>> iterator() {
                        return new Iterator<Entry<String, Object>>() {
                            Iterator<Entry<String, Object>> underlying = filtered.iterator();
                            boolean hasNext = true;

                            @Override
                            public boolean hasNext() {
                                return hasNext;
                            }

                            @Override
                            public Entry<String, Object> next() {
                                if (underlying.hasNext()) {
                                    return underlying.next();
                                }
                                hasNext = false;
                                return idEntry;
                            }
                        };
                    }

                    @Override
                    public int size() {
                        return filtered.size() + 1;
                    }
                };
            }
        };
    }
}
