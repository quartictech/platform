package io.quartic.weyl.core.geofence;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.SpatialIndex;
import io.quartic.weyl.core.feature.FeatureCollection;
import io.quartic.weyl.core.live.LayerView;
import io.quartic.weyl.core.model.AttributeSchema;
import io.quartic.weyl.core.model.Attributes;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureImpl;
import io.quartic.weyl.core.model.Layer;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerImpl;
import io.quartic.weyl.core.model.LayerMetadata;
import io.quartic.weyl.core.model.LayerSpecImpl;
import io.quartic.weyl.core.model.LayerStats;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.subjects.BehaviorSubject;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.mock;

public class LiveLayerChangeAggregatorShould {
    private static final Geometry mockGeometry = mock(Geometry.class);
    private static final LayerId layerIdA = LayerId.fromString("a");
    private static final LayerId layerIdB = LayerId.fromString("b");

    private Map<LayerId, BehaviorSubject<Collection<Feature>>> featureObservables;
    private BehaviorSubject<Collection<Layer>> layers;
    private Observable<LiveLayerChange> liveLayerChanges ;
    private TestSubscriber<LiveLayerChange> sub = TestSubscriber.create();

    @Before
    public void setUp() {
        featureObservables = ImmutableMap.of(
                layerIdA, BehaviorSubject.create(),
                layerIdB, BehaviorSubject.create()
        );
        layers = BehaviorSubject.create();
        liveLayerChanges = LiveLayerChangeAggregator
            .layerChanges(layers, layerId -> featureObservables.get(layerId)
                    .map(features -> liveLayerChange(layerId, features)));

        liveLayerChanges.subscribe(sub);
    }

    @Test
    public void generate_live_layer_changes() {
        Collection<Feature> features = listOf(feature("1"));
        layers.onNext(listOf(layer(layerIdA)));
        featureObservables.get(layerIdA).onNext(features);

        sub.assertReceivedOnNext(listOf(liveLayerChange(layerIdA, features)));
    }

    @Test
    public void not_generate_changes_after_layer_removal() {
        Collection<Feature> features = listOf(feature("1"));
        layers.onNext(listOf(layer(layerIdA)));
        featureObservables.get(layerIdA).onNext(features);

        List<LiveLayerChange> liveLayerChanges = listOf(liveLayerChange(layerIdA, features));
        sub.assertReceivedOnNext(liveLayerChanges);

        // Ubsubscribe all
        layers.onNext(listOf());

        Collection<Feature> features2 = listOf(feature("2"));
        featureObservables.get(layerIdA).onNext(features2);

        // No more changes than before
        sub.assertReceivedOnNext(liveLayerChanges);
    }

    @Test
    public void emit_most_recent_changes() {
        Collection<Feature> features = listOf(feature("1"));
        layers.onNext(listOf());
        featureObservables.get(layerIdA).onNext(features);

        // receive nothing
        sub.assertReceivedOnNext(listOf());

        layers.onNext(listOf(layer(layerIdA)));
        // See most recent change for A
        sub.assertReceivedOnNext(listOf(liveLayerChange(layerIdA, features)));
    }

    @Test
    public void handle_two_layers() {
        Collection<Feature> featuresA = listOf(feature("1"));
        Collection<Feature> featuresB = listOf(feature("2"));

        layers.onNext(listOf(layer(layerIdA), layer(layerIdB)));

        featureObservables.get(layerIdA).onNext(featuresA);
        featureObservables.get(layerIdB).onNext(featuresB);

        sub.assertReceivedOnNext(listOf(liveLayerChange(layerIdA, featuresA), liveLayerChange(layerIdB, featuresB)));
    }


    LiveLayerChange liveLayerChange(LayerId layerId, Collection<Feature> features){
        return LiveLayerChangeImpl.of(layerId, features);
    }

    <T> List<T> listOf(T... args) {
        return ImmutableList.<T>builder().add(args).build();
    }

    Feature feature(String id) {
        return FeatureImpl.of(EntityId.fromString(id), mockGeometry, Attributes.EMPTY_ATTRIBUTES);
    }

    Layer layer(LayerId layerId) {
        return LayerImpl.of(
                LayerSpecImpl.of(
                        layerId,
                        mock(LayerMetadata.class),
                        mock(LayerView.class),
                        mock(AttributeSchema.class),
                        false // Needs to be false so that the layers get picked up
                ),
                FeatureCollection.EMPTY_COLLECTION,
                mock(SpatialIndex.class),
                emptyList(),
                mock(LayerStats.class)
        );
    }

}
