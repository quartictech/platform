package io.quartic.weyl.core.source;

import com.google.common.collect.ImmutableMap;
import io.quartic.catalogue.api.CloudGeoJsonDatasetLocatorImpl;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetConfigImpl;
import io.quartic.catalogue.api.DatasetMetadataImpl;
import io.quartic.weyl.core.model.*;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static io.quartic.weyl.core.live.LayerViewType.LOCATION_AND_TRACK;
import static io.quartic.weyl.core.source.ExtensionCodec.DEFAULT_EXTENSION;
import static io.quartic.weyl.core.source.ExtensionCodec.EXTENSION_KEY;
import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static io.quartic.common.serdes.ObjectMappersKt.objectMapper;

public class ExtensionCodecShould {
    private final ExtensionCodec codec = new ExtensionCodec();

    @Test
    public void return_default_extension_when_not_present() throws Exception {
        assertThat(codec.decode("foo", emptyMap()),
                equalTo(DEFAULT_EXTENSION));
    }

    @Test
    public void return_default_extension_when_unparseable() throws Exception {
        assertThat(codec.decode("foo", ImmutableMap.of(EXTENSION_KEY, "stuff")),
                equalTo(DEFAULT_EXTENSION));
    }

    @Test
    public void return_extension_when_parseable() throws Exception {
        final Map<String, Object> raw = ImmutableMap.of("viewType", "LOCATION_AND_TRACK");

        assertThat(codec.decode("foo", ImmutableMap.of(EXTENSION_KEY, raw)),
                equalTo(MapDatasetExtensionImpl.of(LOCATION_AND_TRACK, StaticSchemaImpl.builder().build())));
    }

    @Test
    public void return_extension_when_parseable_2() throws Exception {
        final Map<String, Object> raw = ImmutableMap.of(
                "viewType", "LOCATION_AND_TRACK",
                "titleAttribute", "foo"
        );

        assertThat(codec.decode("foo", ImmutableMap.of(EXTENSION_KEY, raw)),
                equalTo(MapDatasetExtensionImpl.of(LOCATION_AND_TRACK,
                        StaticSchemaImpl.builder().titleAttribute(AttributeNameImpl.of("foo")).build())));
    }

    @Test
    public void deserialize_attribute_types() {
        final Map<String, Object> raw = ImmutableMap.of(
                "viewType", "LOCATION_AND_TRACK",
                "attributeTypes", ImmutableMap.of("foo", "TIMESTAMP")
        );
        assertThat(codec.decode("foo", ImmutableMap.of(EXTENSION_KEY, raw)),
                equalTo(MapDatasetExtensionImpl.of(LOCATION_AND_TRACK,
                        StaticSchemaImpl.builder()
                                .attributeType(AttributeNameImpl.of("foo"), AttributeType.TIMESTAMP).build())));
    }

    @Test
    public void unparse_to_original() throws IOException {
        MapDatasetExtension extension = MapDatasetExtensionImpl.of(LOCATION_AND_TRACK,
                StaticSchemaImpl.builder()
                        .titleAttribute(AttributeNameImpl.of("test"))
                        .attributeType(AttributeNameImpl.of("foo"), AttributeType.TIMESTAMP)
                .build());
        DatasetConfig datasetConfig = DatasetConfigImpl.of(
                DatasetMetadataImpl.of("foo", "wat", "nope", Optional.empty(), Optional.empty()),
                CloudGeoJsonDatasetLocatorImpl.of("test"),
                codec.encode(extension));

        String json = objectMapper().writeValueAsString(datasetConfig);
        DatasetConfig datasetConfigDeserialized = objectMapper().readValue(json, DatasetConfig.class);
        assertThat(datasetConfig, equalTo(datasetConfigDeserialized));
    }
}
