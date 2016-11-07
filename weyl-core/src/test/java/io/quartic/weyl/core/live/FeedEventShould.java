package io.quartic.weyl.core.live;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.quartic.model.FeedEvent;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class FeedEventShould {
    @Test
    public void deserialize_correctly() throws IOException {
        ObjectMapper OM = new ObjectMapper();
        String json = "{\"source\": \"wat\", \"message\":\"WAT\", \"a\": {\"b\": \"hello\"}}";

        FeedEvent event = OM.readValue(json, FeedEvent.class);
        FeedEvent expected = FeedEvent.of("wat", "WAT", ImmutableMap.of("a", ImmutableMap.of("b", "hello")));

        assertThat(event, equalTo(expected));
    }
}
