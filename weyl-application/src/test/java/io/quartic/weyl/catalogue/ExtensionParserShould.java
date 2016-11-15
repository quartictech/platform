package io.quartic.weyl.catalogue;

import com.google.common.collect.ImmutableMap;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.MapDatasetExtension;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;

import static io.quartic.weyl.catalogue.ExtensionParser.DEFAULT_EXTENSION;
import static io.quartic.weyl.catalogue.ExtensionParser.EXTENSION_KEY;
import static io.quartic.weyl.core.live.LayerViewType.LOCATION_AND_TRACK;
import static java.util.Collections.emptyList;
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
                equalTo(MapDatasetExtension.of(LOCATION_AND_TRACK, Optional.empty(), emptyList())));
    }

    @Test
    public void return_extension_when_parseable_2() throws Exception {
        final Map<String, Object> raw = ImmutableMap.of("viewType", "LOCATION_AND_TRACK", "imageAttribute", "foo");

        assertThat(parser.parse("foo", ImmutableMap.of(EXTENSION_KEY, raw)),
                equalTo(MapDatasetExtension.of(LOCATION_AND_TRACK, Optional.of(AttributeName.of("foo")), emptyList())));
    }
}
