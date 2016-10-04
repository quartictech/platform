package io.quartic.weyl.core.live;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.*;

public class FeedEventShould {
    @Test
    public void deserialize_correctly() throws IOException {
        ObjectMapper OM = new ObjectMapper();
        String json = "{\"source\": \"wat\", \"message\":\"WAT\", \"a\":\"b\"}";

        FeedEvent event = OM.readValue(json, FeedEvent.class);
        FeedEvent expected = FeedEvent.of("wat", "WAT", ImmutableMap.of("a", "b"));

        assertThat(event, equalTo(expected));
    }
}
