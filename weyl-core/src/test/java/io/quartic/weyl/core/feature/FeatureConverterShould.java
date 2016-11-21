package io.quartic.weyl.core.feature;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.geojson.*;
import io.quartic.weyl.core.model.AttributeNameImpl;
import io.quartic.weyl.core.model.AttributesImpl;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.model.NakedFeatureImpl;
import org.junit.Test;

import java.util.Collection;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.weyl.core.utils.GeometryTransformer.webMercatorToWebMercator;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class FeatureConverterShould {
    private final GeometryFactory factory = new GeometryFactory();
    private final FeatureConverter converter = new FeatureConverter(webMercatorToWebMercator());

    @Test
    public void convert_feature_collection() throws Exception {
        final Feature featureA = geojsonFeature("a", Optional.of(point()));
        final Feature featureB = geojsonFeature("b", Optional.of(point()));

        final Collection<NakedFeature> modelFeatures = converter.toModel(FeatureCollectionImpl.of(newArrayList(featureA, featureB)));

        assertThat(modelFeatures, contains(modelFeature("a"), modelFeature("b")));
    }

    @Test
    public void ignore_features_with_null_geometry() throws Exception {
        final Feature feature = geojsonFeature("a", Optional.empty());

        final Collection<NakedFeature> modelFeatures = converter.toModel(FeatureCollectionImpl.of(newArrayList(feature)));

        assertThat(modelFeatures, empty());
    }

    private NakedFeatureImpl modelFeature(String id) {
        return NakedFeatureImpl.of(id, factory.createPoint(new Coordinate(51.0, 0.1)),
                AttributesImpl.builder().attribute(AttributeNameImpl.of("timestamp"), 1234).build());
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
