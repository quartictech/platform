package io.quartic.weyl.core.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quartic.weyl.core.attributes.ComplexAttribute;
import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.Attributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

public class ConversionUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ConversionUtils.class);

    public static Attributes convertToModelAttributes(ObjectMapper objectMapper, Map<String, Object> rawAttributes) {
        final Attributes.Builder builder = Attributes.builder();
        rawAttributes.forEach((k, v) -> builder.attribute(AttributeName.of(k), convertAttributeValue(objectMapper, k, v)));
        return builder.build();
    }

    private static Object convertAttributeValue(ObjectMapper objectMapper, String key, Object value) {
        // TODO: Move this up into generic code behind the importers
        if (value instanceof Map) {
            try {
                return objectMapper.convertValue(value, ComplexAttribute.class);
            }
            catch (IllegalArgumentException e) {
                LOG.warn("couldn't convert attribute {}. Exception: {}", key, e);
                return value;
            }
        }
        return value;
    }

    public static Map<String, Object> convertFromModelAttributes(AbstractFeature feature) {
        final Map<String, Object> output = newHashMap();
        feature.attributes().attributes().entrySet().stream()
                .filter(entry -> !(entry.getValue() instanceof ComplexAttribute))
                .forEach(entry -> output.put(entry.getKey().name(), entry.getValue()));
        output.put("_entityId", feature.entityId().uid());
        return output;
    }
}
