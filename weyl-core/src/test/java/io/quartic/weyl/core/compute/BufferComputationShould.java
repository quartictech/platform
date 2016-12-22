package io.quartic.weyl.core.compute;

import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadataImpl;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BufferComputationShould {

    @Test
    public void produce_valid_metadata() throws Exception {
        final BufferComputation computation = BufferComputationImpl.builder()
                .layerId(mock(LayerId.class))
                .bufferSpec(BufferSpecImpl.of(mock(LayerId.class), 25.0))
                .clock(Clock.fixed(Instant.EPOCH, ZoneId.systemDefault()))
                .build();
        final Layer layer = mock(Layer.class, RETURNS_DEEP_STUBS);
        when(layer.spec().metadata().name()).thenReturn("Foo");
        when(layer.spec().metadata().description()).thenReturn("Bar");
        when(layer.spec().metadata().attribution()).thenReturn("Quartic");

        assertThat(computation.spec(newArrayList(layer)).metadata(), equalTo(LayerMetadataImpl.of(
                "Foo (buffered)",
                "Bar (buffered by 25.0m)",
                "Quartic",
                Instant.EPOCH,
                Optional.empty()
        )));

    }
}
