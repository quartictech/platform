package io.quartic.weyl.core.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quartic.weyl.core.attributes.ComplexAttribute;
import io.quartic.weyl.core.model.AttributeName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class ConversionUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ConversionUtils.class);

    public static Map<AttributeName, Object> convertMetadata(ObjectMapper objectMapper, Map<String, Object> rawMetadata) {
        return rawMetadata.entrySet()
                .stream()
                .collect(toMap(
                        e -> AttributeName.of(e.getKey()),
                        e -> ConversionUtils.convertMetadataValue(objectMapper, e.getKey(), e.getValue())));
    }

    static Object convertMetadataValue(ObjectMapper objectMapper, String key, Object value) {
        // TODO: Move this up into generic code behind the importers
        if (value instanceof Map) {
            try {
                return objectMapper.convertValue(value, ComplexAttribute.class);
            }
            catch (IllegalArgumentException e) {
                LOG.warn("couldn't convert object field {}. Exception: {}", key, e);
                return value;
            }
        }
        return value;
    }
}
