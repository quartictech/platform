package io.quartic.weyl.core.source;

import io.quartic.weyl.core.model.MapDatasetExtension;
import io.quartic.weyl.core.model.MapDatasetExtensionImpl;
import io.quartic.weyl.core.model.StaticSchemaImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static io.quartic.common.serdes.ObjectMappers.OBJECT_MAPPER;
import static java.lang.String.format;

public class ExtensionParser {
    public static final String EXTENSION_KEY = "map";
    public static final MapDatasetExtension DEFAULT_EXTENSION = MapDatasetExtensionImpl.builder()
            .staticSchema(StaticSchemaImpl.builder().build())
            .build();
    private static final Logger LOG = LoggerFactory.getLogger(ExtensionParser.class);

    public MapDatasetExtension parse(String name, Map<String, Object> extensions) {
        final Object extension = extensions.get(EXTENSION_KEY);
        if (extension != null) {
            try {
                return INSTANCE.getOBJECT_MAPPER().convertValue(extension, MapDatasetExtension.class);
            } catch (IllegalArgumentException e) {
                LOG.warn(format("[%s] Unable to interpret extension, so using default", name), e);
            }
        } else {
            LOG.info(format("[%s] No extension found, so using default", name));
        }
        return DEFAULT_EXTENSION;
    }

}
