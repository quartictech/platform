package io.quartic.weyl.core.live;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.geojson.*;
import io.quartic.weyl.core.model.AttributeNameImpl;
import io.quartic.weyl.core.model.AttributesImpl;
import io.quartic.weyl.core.model.NakedFeatureImpl;
import io.quartic.weyl.core.source.SourceUpdate;
import org.junit.Test;

import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.weyl.core.utils.GeometryTransformer.webMercatorToWebMercator;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class LiveEventConverterShould {
    private final GeometryFactory factory = new GeometryFactory();
    private final LiveEventConverter converter = new LiveEventConverter(webMercatorToWebMercator());

    // TODO: multiple LiveEvents

    @Test
    public void convert_features() throws Exception {
        final FeatureCollection collection = featureCollectionOf(geojsonFeature("a", Optional.of(point())));

        final SourceUpdate update = converter.updateFrom(collection);

        assertThat(update.features(), equalTo(ImmutableList.of(
                NakedFeatureImpl.of("a", factory.createPoint(new Coordinate(51.0, 0.1)),
                        AttributesImpl.builder().attribute(AttributeNameImpl.of("timestamp"), 1234).build())
        )));
    }

    @Test
    public void ignore_features_with_null_geometry() throws Exception {
        final FeatureCollection collection = featureCollectionOf(geojsonFeature("a", Optional.empty()));

        final SourceUpdate update = converter.updateFrom(collection);

        assertThat(update.features(), empty());
    }

    private FeatureCollection featureCollectionOf(Feature... features) {
        return FeatureCollectionImpl.of(newArrayList(features));
    }

    private Feature geojsonFeature(String id, Optional<Geometry> geometry) {
        return FeatureImpl.of(
                Optional.of(id),
                geometry,
                ImmutableMap.of("timestamp", 1234));
    }

    private Point point() {
        return point(51.0, 0.1);
    }

    private Point point(double x, double y) {
        return PointImpl.of(ImmutableList.of(x, y));
    }
}
