package io.quartic.weyl.resource;

import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import io.quartic.weyl.core.model.SnapshotImpl;
import io.quartic.weyl.core.render.VectorTileRenderer;
import net.bytebuddy.implementation.bytecode.Throw;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;

import java.util.Objects;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class TileResourceShould {

    private final PublishSubject<LayerSnapshotSequence> snapshotSequences = PublishSubject.create();
    private final VectorTileRenderer renderer = mock(VectorTileRenderer.class);
    private final TileResource resource = TileResourceImpl.builder()
            .snapshotSequences(snapshotSequences)
            .renderer(renderer)
            .build();

    private final LayerId layerId = mock(LayerId.class);

    @Test
    public void render_layer_when_present() throws Exception {
        final Layer layer = mock(Layer.class);
        final byte[] expected = { 1, 2, 3 };
        mockRendererResult(expected);
        nextSequence(layerId).onNext(snapshot(layer));

        final byte[] bytes = renderBytes(layerId);

        verify(renderer).render(layer, 5, 6, 7);
        assertThat(bytes, equalTo(expected));
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

        renderBytes(layerId);

        verify(renderer).render(layer2, 5, 6, 7);
    }

    @Test
    public void return_no_content_when_no_features_to_be_rendered() throws Exception {
        mockRendererResult(new byte[] {});
        nextSequence(layerId).onNext(snapshot(mock(Layer.class)));

        final Response response = renderResponse(layerId);

        assertThat(response.getStatus(), equalTo(204));
    }

    @Test
    public void respond_with_4xx_when_layer_not_present() throws Exception {
        Throwable throwable = renderThrowable(layerId);
        assertThat(throwable, instanceOf(NotFoundException.class));
    }

    private <T> T render(LayerId layerId, Class<T> responseClass) {
        ArgumentCaptor<T> argumentCaptor = ArgumentCaptor.forClass(responseClass);
        AsyncResponse asyncResponse = mock(AsyncResponse.class);
        resource.render(layerId, 5, 6, 7, asyncResponse);
        verify(asyncResponse, timeout(5000)).resume(argumentCaptor.capture());
        T response =  argumentCaptor.getValue();
        assertThat(response, instanceOf(responseClass));
        return response;
    }

    // NOTE: Duplication with above is necessary. AsyncResponse.resume() is overloaded and one version takes Object,
    // another takes Throwable. To ensure the correct method is verified by Mockito, I have to explicitly specify
    // Throwable. Trying to use generics makes everything Object at compile time and dispatches to resume(Object) instead.
    // :-(
     private Throwable renderThrowable(LayerId layerId) {
        ArgumentCaptor<Throwable> argumentCaptor = ArgumentCaptor.forClass(Throwable.class);
        AsyncResponse asyncResponse = mock(AsyncResponse.class);
        resource.render(layerId, 5, 6, 7, asyncResponse);
        verify(asyncResponse, timeout(5000)).resume(argumentCaptor.capture());
        Throwable response =  argumentCaptor.getValue();
        assertThat(response, instanceOf(Throwable.class));
        return response;
    }

    private byte[] renderBytes(LayerId layerId) {
        return render(layerId, byte[].class);
    }

    private Response renderResponse(LayerId layerId) {
        return render(layerId, Response.class);
    }

    private BehaviorSubject<Snapshot> nextSequence(LayerId layerId) {
        final BehaviorSubject<Snapshot> snapshots = BehaviorSubject.create();
        final LayerSnapshotSequence seq = mock(LayerSnapshotSequence.class, RETURNS_DEEP_STUBS);
        when(seq.spec().id()).thenReturn(layerId);
        when(seq.snapshots()).thenReturn(snapshots);
        snapshotSequences.onNext(seq);
        return snapshots;
    }

    private Snapshot snapshot(Layer layer) {
        return SnapshotImpl.of(layer, emptyList());
    }

    private void mockRendererResult(byte[] expected) {
        when(renderer.render(any(), anyInt(), anyInt(), anyInt())).thenReturn(expected);
    }
}
