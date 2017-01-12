package io.quartic.weyl.core.source;

import com.google.common.collect.ImmutableMap;
import io.quartic.weyl.core.model.AttributeNameImpl;
import io.quartic.weyl.core.model.AttributeType;
import io.quartic.weyl.core.model.MapDatasetExtensionImpl;
import io.quartic.weyl.core.model.StaticSchemaImpl;
import org.junit.Test;

import java.util.Map;

import static io.quartic.weyl.core.live.LayerViewType.LOCATION_AND_TRACK;
import static io.quartic.weyl.core.source.ExtensionParser.DEFAULT_EXTENSION;
import static io.quartic.weyl.core.source.ExtensionParser.EXTENSION_KEY;
import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class ExtensionParserShould {
    private final ExtensionParser parser = new ExtensionParser();

    @Test
    public void return_default_extension_when_not_present() throws Exception {
        assertThat(parser.parse("foo", emptyMap()),
                equalTo(DEFAULT_EXTENSION));
    }

    @Test
    public void return_default_extension_when_unparseable() throws Exception {
        assertThat(parser.parse("foo", ImmutableMap.of(EXTENSION_KEY, "stuff")),
                equalTo(DEFAULT_EXTENSION));
    }

    @Test
    public void return_extension_when_parseable() throws Exception {
        final Map<String, Object> raw = ImmutableMap.of("viewType", "LOCATION_AND_TRACK");

        assertThat(parser.parse("foo", ImmutableMap.of(EXTENSION_KEY, raw)),
                equalTo(MapDatasetExtensionImpl.of(LOCATION_AND_TRACK, StaticSchemaImpl.builder().build())));
    }

    @Test
    public void return_extension_when_parseable_2() throws Exception {
        final Map<String, Object> raw = ImmutableMap.of(
                "viewType", "LOCATION_AND_TRACK",
                "titleAttribute", "foo"
        );

        assertThat(parser.parse("foo", ImmutableMap.of(EXTENSION_KEY, raw)),
                equalTo(MapDatasetExtensionImpl.of(LOCATION_AND_TRACK,
                        StaticSchemaImpl.builder().titleAttribute(AttributeNameImpl.of("foo")).build())));
    }

    @Test
    public void deserialize_attribute_types() {
        final Map<String, Object> raw = ImmutableMap.of(
                "viewType", "LOCATION_AND_TRACK",
                "attributeTypes", ImmutableMap.of("foo", "TIMESTAMP")
        );
        assertThat(parser.parse("foo", ImmutableMap.of(EXTENSION_KEY, raw)),
                equalTo(MapDatasetExtensionImpl.of(LOCATION_AND_TRACK,
                        StaticSchemaImpl.builder()
                                .attributeType(AttributeNameImpl.of("foo"), AttributeType.TIMESTAMP).build())));
    }
}
