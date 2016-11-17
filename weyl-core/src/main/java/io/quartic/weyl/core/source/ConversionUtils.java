package io.quartic.weyl.core.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quartic.weyl.core.attributes.ComplexAttribute;
import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.AttributeName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.stream.Collectors.toMap;

public class ConversionUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ConversionUtils.class);

    public static Map<AttributeName, Object> convertToModelAttributes(ObjectMapper objectMapper, Map<String, Object> rawAttributes) {
        return rawAttributes.entrySet()
                .stream()
                .collect(toMap(
                        e -> AttributeName.of(e.getKey()),
                        e -> convertAttributeValue(objectMapper, e.getKey(), e.getValue())));
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
        feature.attributes().entrySet().stream()
                .filter(entry -> !(entry.getValue() instanceof ComplexAttribute))
                .forEach(entry -> output.put(entry.getKey().name(), entry.getValue()));
        output.put("_id", feature.uid().uid());  // TODO: eliminate the _id concept
        output.put("_externalId", feature.entityId().uid());
        return output;
    }
}
