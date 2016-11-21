package io.quartic.weyl.catalogue;

import io.quartic.weyl.core.model.MapDatasetExtension;
import io.quartic.weyl.core.model.MapDatasetExtensionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static io.quartic.common.serdes.ObjectMappers.OBJECT_MAPPER;
import static java.lang.String.format;

class ExtensionParser {
    public static final String EXTENSION_KEY = "map";
    public static final MapDatasetExtension DEFAULT_EXTENSION = MapDatasetExtensionImpl.builder().build();
    private static final Logger LOG = LoggerFactory.getLogger(ExtensionParser.class);

    MapDatasetExtension parse(String name, Map<String, Object> extensions) {
        final Object extension = extensions.get(EXTENSION_KEY);
        if (extension == null) {
            LOG.info(format("[%s] No extension found, so using default", name));
            return MapDatasetExtensionImpl.builder().build();
        }

        try {
            return OBJECT_MAPPER.convertValue(extension, MapDatasetExtension.class);
        } catch (IllegalArgumentException e) {
            LOG.warn(format("[%s] Unable to interpret extension, so using default", name), e);
            return MapDatasetExtensionImpl.builder().build();
        }
    }

}
