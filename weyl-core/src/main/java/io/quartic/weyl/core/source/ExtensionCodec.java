package io.quartic.weyl.core.source;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import io.quartic.weyl.core.model.MapDatasetExtension;
import io.quartic.weyl.core.model.StaticSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static io.quartic.common.serdes.ObjectMappersKt.objectMapper;
import static java.lang.String.format;

public class ExtensionCodec {
    public static final String EXTENSION_KEY = "map";
    public static final MapDatasetExtension DEFAULT_EXTENSION = new MapDatasetExtension(new StaticSchema());
    private static final Logger LOG = LoggerFactory.getLogger(ExtensionCodec.class);

    public MapDatasetExtension decode(String name, Map<String, Object> extensions) {
        final Object extension = extensions.get(EXTENSION_KEY);
        if (extension != null) {
            try {
                return objectMapper().convertValue(extension, MapDatasetExtension.class);
            } catch (IllegalArgumentException e) {
                LOG.warn(format("[%s] Unable to interpret extension, so using default", name), e);
            }
        } else {
            LOG.info(format("[%s] No extension found, so using default", name));
        }
        return DEFAULT_EXTENSION;
    }

    public Map<String, Object> encode(MapDatasetExtension extension) {
        return ImmutableMap.of(EXTENSION_KEY, objectMapper().convertValue(extension,
                new TypeReference<Map<String, Object>>() { }));
    }

}
