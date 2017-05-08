package io.quartic.weyl.resource;

import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Diff;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import io.quartic.weyl.core.model.SnapshotId;
import io.quartic.weyl.core.render.VectorTileRenderer;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import java.util.function.Consumer;

import static io.quartic.weyl.api.LayerUpdateType.APPEND;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TileResourceShould {
    private final PublishSubject<LayerSnapshotSequence> snapshotSequences = PublishSubject.create();
    private final VectorTileRenderer renderer = mock(VectorTileRenderer.class);
    private final TileResource resource = new TileResource(snapshotSequences, renderer);
    private final LayerId layerId = mock(LayerId.class);

    @Test
    public void render_layer_when_present() throws Exception {
        final Layer layer = mock(Layer.class);
        final byte[] expected = { 1, 2, 3 };
        mockRendererResult(expected);
        nextSequence(layerId).onNext(snapshot(layer));

        render(layerId, asyncResponse -> {
            ArgumentCaptor<byte[]> argumentCaptor = ArgumentCaptor.forClass(byte[].class);
            verify(asyncResponse, timeout(5000)).resume(argumentCaptor.capture());
            assertThat(argumentCaptor.getValue(), instanceOf(byte[].class));
            assertThat(argumentCaptor.getValue(), equalTo(expected));
        });
    }

    @Test
    public void render_latest_layer_snapshot() throws Exception {
        final Layer layer1 = mock(Layer.class);
        final Layer layer2 = mock(Layer.class);
        final byte[] expected = { 1, 2, 3 };
        mockRendererResult(expected);

        final BehaviorSubject<Snapshot> snapshots = nextSequence(layerId);
        snapshots.onNext(snapshot(layer1));
        snapshots.onNext(snapshot(layer2));

        render(layerId, asyncResponse -> verify(renderer, timeout(5000)).render(layer2, 5, 6, 7));
    }

    @Test
    public void return_no_content_when_no_features_to_be_rendered() throws Exception {
        mockRendererResult(new byte[] {});
        nextSequence(layerId).onNext(snapshot(mock(Layer.class)));

        render(layerId, asyncResponse -> {
            ArgumentCaptor<Response> argumentCaptor = ArgumentCaptor.forClass(Response.class);
            verify(asyncResponse, timeout(5000)).resume(argumentCaptor.capture());
            assertThat(argumentCaptor.getValue(), instanceOf(Response.class));
            assertThat(argumentCaptor.getValue().getStatus(), equalTo(204));
        });
    }

    @Test
    public void respond_with_4xx_when_layer_not_present() throws Exception {
        render(layerId, asyncResponse -> {
            ArgumentCaptor<Throwable> argumentCaptor = ArgumentCaptor.forClass(Throwable.class);
            verify(asyncResponse, timeout(5000)).resume(argumentCaptor.capture());
            assertThat(argumentCaptor.getValue(), instanceOf(NotFoundException.class));
        });
    }

    private void render(LayerId layerId, Consumer<AsyncResponse> verifier) {
        AsyncResponse asyncResponse = mock(AsyncResponse.class);
        resource.render(layerId, mock(SnapshotId.class),5, 6, 7, asyncResponse);
        verifier.accept(asyncResponse);
    }

    private BehaviorSubject<Snapshot> nextSequence(LayerId layerId) {
        final BehaviorSubject<Snapshot> snapshots = BehaviorSubject.create();
        final LayerSnapshotSequence seq = mock(LayerSnapshotSequence.class, RETURNS_DEEP_STUBS);
        when(seq.getSpec().getId()).thenReturn(layerId);
        when(seq.getSnapshots()).thenReturn(snapshots);
        snapshotSequences.onNext(seq);
        return snapshots;
    }

    private Snapshot snapshot(Layer layer) {
        return new Snapshot(mock(SnapshotId.class), layer, new Diff(APPEND, emptyList()));
    }

    private void mockRendererResult(byte[] expected) {
        when(renderer.render(any(), anyInt(), anyInt(), anyInt())).thenReturn(expected);
    }
}
