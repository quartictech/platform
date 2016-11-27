package io.quartic.weyl.core.feature;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.geojson.Feature;
import io.quartic.geojson.*;
import io.quartic.geojson.FeatureImpl;
import io.quartic.weyl.core.attributes.AttributesFactory;
import io.quartic.weyl.core.model.*;
import org.junit.Test;

import java.util.Collection;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.common.CollectionUtils.entry;
import static io.quartic.common.CollectionUtils.map;
import static io.quartic.weyl.core.feature.FeatureConverter.getRawProperties;
import static io.quartic.weyl.core.utils.GeometryTransformer.webMercatorToWebMercator;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class FeatureConverterShould {
    private final GeometryFactory factory = new GeometryFactory();
    private final AttributesFactory attributesFactory = mock(AttributesFactory.class);
    private final FeatureConverter converter = new FeatureConverter(attributesFactory, webMercatorToWebMercator());

    @Test
    public void convert_feature_collection() throws Exception {
        final Attributes attributesA = mock(Attributes.class);
        final Attributes attributesB = mock(Attributes.class);
        final AttributesFactory.AttributesBuilder builderA = attributesBuilder(attributesA);
        final AttributesFactory.AttributesBuilder builderB = attributesBuilder(attributesB);
        when(attributesFactory.builder())
                .thenReturn(builderA)
                .thenReturn(builderB);
        final Feature featureA = geojsonFeature("a", Optional.of(point()));
        final Feature featureB = geojsonFeature("b", Optional.of(point()));

        final Collection<NakedFeature> modelFeatures = converter.toModel(FeatureCollectionImpl.of(newArrayList(featureA, featureB)));

        verify(attributesFactory, times(2)).builder();
        verify(builderA).put("timestamp", 1234);
        verify(builderB).put("timestamp", 1234);
        assertThat(modelFeatures, contains(modelFeature("a", attributesA), modelFeature("b", attributesB)));
    }

    @Test
    public void ignore_features_with_null_geometry() throws Exception {
        final Feature feature = geojsonFeature("a", Optional.empty());

        final Collection<NakedFeature> modelFeatures = converter.toModel(FeatureCollectionImpl.of(newArrayList(feature)));

        assertThat(modelFeatures, empty());
    }

    @Test
    public void strip_null_attributes_when_creating_raw() throws Exception {
        io.quartic.weyl.core.model.Feature feature = io.quartic.weyl.core.model.FeatureImpl.of(
                EntityId.fromString("a"),
                mock(com.vividsolutions.jts.geom.Geometry.class),
                () -> map(
                        entry("timestamp", 1234),
                        entry("noob", null))
        );

        assertThat(getRawProperties(feature), equalTo(map(
                entry("_entityId", "a"),
                entry("timestamp", 1234)
        )));
    }

    private AttributesFactory.AttributesBuilder attributesBuilder(Attributes attributes) {
        final AttributesFactory.AttributesBuilder builder = mock(AttributesFactory.AttributesBuilder.class);
        when(builder.build()).thenReturn(attributes);
        return builder;
    }

    private NakedFeatureImpl modelFeature(String id, Attributes attributes) {
        return NakedFeatureImpl.of(Optional.of(id), factory.createPoint(new Coordinate(51.0, 0.1)), attributes);
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
