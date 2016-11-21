package io.quartic.weyl.core.feature;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quartic.weyl.core.attributes.ComplexAttribute;
import io.quartic.weyl.core.geojson.Utils;
import io.quartic.weyl.core.model.*;
import io.quartic.weyl.core.utils.GeometryTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static io.quartic.common.serdes.ObjectMappers.OBJECT_MAPPER;

public class FeatureConverter {
    private static final Logger LOG = LoggerFactory.getLogger(FeatureConverter.class);

    private final GeometryTransformer geometryTransformer;

    public FeatureConverter(GeometryTransformer geometryTransformer) {
        this.geometryTransformer = geometryTransformer;
    }

    public NakedFeature toModel(io.quartic.geojson.Feature f) {
        // HACK: we can assume that we've simply filtered out features with null geometries for now
        return NakedFeatureImpl.of(
                f.id().get(),
                geometryTransformer.transform(Utils.toJts(f.geometry().get())),
                convertToModelAttributes(OBJECT_MAPPER, f.properties())
        );
    }


    public static Attributes convertToModelAttributes(ObjectMapper objectMapper, Map<String, Object> rawAttributes) {
        final AttributesImpl.Builder builder = AttributesImpl.builder();
        rawAttributes.forEach((k, v) -> builder.attribute(AttributeNameImpl.of(k), convertAttributeValue(objectMapper, k, v)));
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

    public static Map<String, Object> convertFromModelAttributes(Feature feature) {
        final Map<String, Object> output = newHashMap();
        feature.attributes().attributes().entrySet().stream()
                .filter(entry -> !(entry.getValue() instanceof ComplexAttribute))
                .forEach(entry -> output.put(entry.getKey().name(), entry.getValue()));
        output.put("_entityId", feature.entityId().uid());
        return output;
    }
}
