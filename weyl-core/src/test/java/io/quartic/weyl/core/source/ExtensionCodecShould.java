package io.quartic.weyl.core.source;

import com.google.common.collect.ImmutableMap;
import io.quartic.catalogue.api.model.CloudGeoJsonDatasetLocator;
import io.quartic.catalogue.api.model.DatasetConfig;
import io.quartic.catalogue.api.model.DatasetMetadata;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.AttributeType;
import io.quartic.weyl.core.model.MapDatasetExtension;
import io.quartic.weyl.core.model.StaticSchema;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static io.quartic.common.serdes.ObjectMappersKt.objectMapper;
import static io.quartic.weyl.core.live.LayerViewType.LOCATION_AND_TRACK;
import static io.quartic.weyl.core.source.ExtensionCodec.DEFAULT_EXTENSION;
import static io.quartic.weyl.core.source.ExtensionCodec.EXTENSION_KEY;
import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

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
                equalTo(new MapDatasetExtension(new StaticSchema(), LOCATION_AND_TRACK)));
    }

    @Test
    public void return_extension_when_parseable_2() throws Exception {
        final Map<String, Object> raw = ImmutableMap.of(
                "viewType", "LOCATION_AND_TRACK",
                "titleAttribute", "foo"
        );

        assertThat(codec.decode("foo", ImmutableMap.of(EXTENSION_KEY, raw)),
                equalTo(new MapDatasetExtension(new StaticSchema(new AttributeName("foo")), LOCATION_AND_TRACK)));
    }

    @Test
    public void deserialize_attribute_types() {
        final Map<String, Object> raw = ImmutableMap.of(
                "viewType", "LOCATION_AND_TRACK",
                "attributeTypes", ImmutableMap.of("foo", "TIMESTAMP")
        );
        assertThat(codec.decode("foo", ImmutableMap.of(EXTENSION_KEY, raw)),
                equalTo(new MapDatasetExtension(
                        new StaticSchema(
                                null,
                                null,
                                null,
                                null,
                                null,
                                ImmutableMap.of(new AttributeName("foo"), AttributeType.TIMESTAMP)),
                        LOCATION_AND_TRACK
                        )));
    }

    @Test
    public void unparse_to_original() throws IOException {
        MapDatasetExtension extension = new MapDatasetExtension(
                new StaticSchema(
                        new AttributeName("test"),
                        null,
                        null,
                        null,
                        null,
                        ImmutableMap.of(new AttributeName("foo"), AttributeType.TIMESTAMP)),
                LOCATION_AND_TRACK);
        DatasetConfig datasetConfig = new DatasetConfig(
                new DatasetMetadata("foo", "wat", "nope", null),
                new CloudGeoJsonDatasetLocator("test", false),
                codec.encode(extension));

        String json = objectMapper().writeValueAsString(datasetConfig);
        DatasetConfig datasetConfigDeserialized = objectMapper().readValue(json, DatasetConfig.class);
        assertThat(datasetConfigDeserialized, equalTo(datasetConfig));
    }
}
