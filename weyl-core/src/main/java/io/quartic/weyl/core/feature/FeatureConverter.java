package io.quartic.weyl.core.feature;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quartic.weyl.core.attributes.AttributesFactory;
import io.quartic.weyl.core.attributes.ComplexAttribute;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.core.model.*;
import io.quartic.weyl.core.utils.GeometryTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static io.quartic.common.serdes.ObjectMappers.OBJECT_MAPPER;
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

    public static Map<String, Object> getRawProperties(Feature feature) {
        final Map<String, Object> output = newHashMap();
        feature.attributes().attributes().entrySet().stream()
                .filter(entry -> !(entry.getValue() instanceof ComplexAttribute) && (entry.getValue() != null))
                .forEach(entry -> output.put(entry.getKey().name(), entry.getValue()));
        output.put("_entityId", feature.entityId().uid());
        return output;
    }
}
