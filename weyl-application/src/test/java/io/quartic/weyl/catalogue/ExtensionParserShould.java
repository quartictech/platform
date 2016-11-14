package io.quartic.weyl.catalogue;

import com.google.common.collect.ImmutableMap;
import io.quartic.weyl.core.model.MapDatasetExtension;
import org.junit.Test;

import java.util.Map;

import static io.quartic.weyl.catalogue.ExtensionParser.DEFAULT_EXTENSION;
import static io.quartic.weyl.catalogue.ExtensionParser.EXTENSION_KEY;
import static io.quartic.weyl.common.serdes.ObjectMappers.OBJECT_MAPPER;
import static io.quartic.weyl.core.live.LayerViewType.LOCATION_AND_TRACK;
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
        final MapDatasetExtension expected = MapDatasetExtension.of(LOCATION_AND_TRACK);

        assertThat(parser.parse("foo", ImmutableMap.of(EXTENSION_KEY, OBJECT_MAPPER.convertValue(expected, Map.class))),
                equalTo(expected));
    }
}
