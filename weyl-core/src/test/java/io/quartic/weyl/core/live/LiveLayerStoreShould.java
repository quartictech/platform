package io.quartic.weyl.core.live;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.quartic.weyl.core.geojson.Feature;
import io.quartic.weyl.core.geojson.FeatureCollection;
import io.quartic.weyl.core.geojson.Geometry;
import io.quartic.weyl.core.geojson.Point;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
import org.junit.Test;

import java.util.Collection;
import java.util.Optional;

import static io.quartic.weyl.core.geojson.Utils.lineStringFrom;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class LiveLayerStoreShould {
    private final LiveLayerStore store = new LiveLayerStore();

    @Test
    public void create_a_layer() throws Exception {
        LayerId id = store.createLayer(LayerMetadata.of("foo", "bar"));

        assertThat(id, notNullValue());
    }

    @Test
    public void list_created_layers() throws Exception {
        final LayerMetadata lm1 = LayerMetadata.of("foo", "bar");
        final LayerMetadata lm2 = LayerMetadata.of("cheese", "monkey");

        LayerId id1 = store.createLayer(lm1);
        LayerId id2 = store.createLayer(lm2);

        final Collection<LiveLayer> layers = store.listLayers();

        assertThat(layers.stream().map(LiveLayer::layerId).collect(toList()),
                containsInAnyOrder(id1, id2));
        assertThat(layers.stream().map(l -> l.layer().metadata()).collect(toList()),
                containsInAnyOrder(lm1, lm2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_if_adding_to_non_existent_layer() throws Exception {
        store.addToLayer(LayerId.of("abcd"), featureCollection(feature("a", point())));
    }

    @Test
    public void accept_if_adding_to_existing_layer() throws Exception {
        LayerId id = store.createLayer(LayerMetadata.of("foo", "bar"));

        store.addToLayer(id, featureCollection(featureWithId("a", point())));
    }

    @Test
    public void return_features_added_to_layer() throws Exception {
        LayerId id = store.createLayer(LayerMetadata.of("foo", "bar"));

        store.addToLayer(id, featureCollection(feature("a", point())));

        assertThat(store.getFeaturesForLayer(id),
                equalTo(featureCollection(
                        featureWithId("a", point())
                ))
        );
    }

    @Test
    public void return_extra_features_added_to_layer() throws Exception {
        LayerId id = store.createLayer(LayerMetadata.of("foo", "bar"));

        store.addToLayer(id, featureCollection(feature("a", point())));
        store.addToLayer(id, featureCollection(feature("b", point())));

        assertThat(store.getFeaturesForLayer(id),
                equalTo(featureCollection(
                        featureWithId("a", point()),
                        featureWithId("b", point())
                ))
        );
    }

    @Test
    public void return_newest_feature_and_history_for_particular_id() throws Exception {
        LayerId id = store.createLayer(LayerMetadata.of("foo", "bar"));

        store.addToLayer(id, featureCollection(feature("a", point(1.0, 2.0))));
        store.addToLayer(id, featureCollection(feature("a", point(3.0, 4.0))));

        assertThat(store.getFeaturesForLayer(id),
                equalTo(featureCollection(
                        featureWithId("a", point(3.0, 4.0)),
                        featureWithId("a", lineStringFrom(point(1.0, 2.0), point(3.0, 4.0)))
                ))
        );
    }
    
    private FeatureCollection featureCollection(Feature... features) {
        return FeatureCollection.of(ImmutableList.copyOf(features));
    }

    private Feature feature(String id, Geometry geometry) {
        return Feature.of(Optional.of(id), geometry, ImmutableMap.of("timestamp", 1234));
    }

    private Feature featureWithId(String id, Geometry geometry) {
        return Feature.of(Optional.of(id), geometry, ImmutableMap.of("timestamp", 1234, "_id", id));
    }

    private Point point() {
        return point(123.0, 456.0);
    }

    private Point point(double x, double y) {
        return Point.of(ImmutableList.of(x, y));
    }
}
