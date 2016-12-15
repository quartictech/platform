package io.quartic.weyl.resource;

import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerSnapshotSequence;
import io.quartic.weyl.core.model.LayerSnapshotSequence.Snapshot;
import io.quartic.weyl.core.model.SnapshotImpl;
import io.quartic.weyl.core.render.VectorTileRenderer;
import org.junit.Test;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

import javax.ws.rs.NotFoundException;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

        final byte[] bytes = render(layerId);

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

        render(layerId);

        verify(renderer).render(layer2, 5, 6, 7);
    }

    @Test
    public void return_null_when_no_features_to_be_rendered() throws Exception {
        mockRendererResult(new byte[] {});
        nextSequence(layerId).onNext(snapshot(mock(Layer.class)));

        final byte[] bytes = render(layerId);

        assertThat(bytes, equalTo(null));
    }

    @Test(expected = NotFoundException.class)
    public void respond_with_4xx_when_layer_not_present() throws Exception {
        render(layerId);
    }

    @Test(expected = NotFoundException.class)
    public void respond_with_4xx_when_layer_complete() throws Exception {
        nextSequence(layerId).onCompleted();

        render(layerId);
    }

    private byte[] render(LayerId layerId) {
        return resource.render(layerId, 5, 6, 7);
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
