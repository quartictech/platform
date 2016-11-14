package io.quartic.weyl.core.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quartic.weyl.core.attributes.ComplexAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Collectors;

public class ConversionUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ConversionUtils.class);

    public static Map<String, Object> convertMetadata(ObjectMapper objectMapper, Map<String, Object> rawMetadata) {
        return rawMetadata.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> ConversionUtils.convertMetadataValue(objectMapper, entry.getValue())));
    }

    static Object convertMetadataValue(ObjectMapper objectMapper, Object value) {
        // TODO: Move this up into generic code behind the importers
        if (value instanceof Map) {
            System.out.println(value);
            try {
                return objectMapper.convertValue(value, ComplexAttribute.class);
            }
            catch (IllegalArgumentException e) {
                LOG.info("balls: " + e);
                return value;
            }
        }
        return value;
    }
}
