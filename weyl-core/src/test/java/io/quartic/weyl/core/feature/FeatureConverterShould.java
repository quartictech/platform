package io.quartic.weyl.core.feature;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.common.geojson.*;
import io.quartic.common.geojson.FeatureCollection;
import io.quartic.weyl.core.attributes.AttributesFactory;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.AttributeNameImpl;
import io.quartic.weyl.core.model.Attributes;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.model.NakedFeatureImpl;
import org.junit.Test;

import java.util.Collection;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.common.test.CollectionUtilsKt.entry;
import static io.quartic.common.test.CollectionUtilsKt.map;
import static io.quartic.weyl.core.feature.FeatureConverter.getRawProperties;
import static io.quartic.weyl.core.utils.GeometryTransformer.webMercatorToWebMercator;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FeatureConverterShould {
    private final GeometryFactory factory = new GeometryFactory();
    private final AttributesFactory attributesFactory = mock(AttributesFactory.class);
    private final FeatureConverter converter = new FeatureConverter(attributesFactory, webMercatorToWebMercator(), webMercatorToWebMercator());

    @Test
    public void convert_feature_collection() throws Exception {
        final Attributes attributesA = mock(Attributes.class);
        final Attributes attributesB = mock(Attributes.class);
        final AttributesFactory.AttributesBuilder builderA = attributesBuilder(attributesA);
        final AttributesFactory.AttributesBuilder builderB = attributesBuilder(attributesB);
        when(attributesFactory.builder())
                .thenReturn(builderA)
                .thenReturn(builderB);
        final Feature featureA = geojsonFeature("a", point());
        final Feature featureB = geojsonFeature("b", point());

        final Collection<NakedFeature> modelFeatures = converter.toModel(new FeatureCollection(newArrayList(featureA, featureB)));

        verify(attributesFactory, times(2)).builder();
        verify(builderA).put("timestamp", 1234);
        verify(builderB).put("timestamp", 1234);
        assertThat(modelFeatures, contains(modelFeature("a", attributesA), modelFeature("b", attributesB)));
    }

    @Test
    public void ignore_features_with_null_geometry() throws Exception {
        final Feature feature = geojsonFeature("a", null);

        final Collection<NakedFeature> modelFeatures = converter.toModel(new FeatureCollection(newArrayList(feature)));

        assertThat(modelFeatures, empty());
    }

    @Test
    public void strip_null_attributes_when_creating_raw() throws Exception {
        io.quartic.weyl.core.model.Feature feature = io.quartic.weyl.core.model.FeatureImpl.of(
                EntityId.fromString("a"),
                mock(com.vividsolutions.jts.geom.Geometry.class),
                () -> map(
                        entry(name("timestamp"), 1234),
                        entry(name("noob"), null))
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

    private Feature geojsonFeature(String id, Geometry geometry) {
        return new Feature(id, geometry, ImmutableMap.of("timestamp", 1234));
    }

    private Point point() {
        return point(51.0, 0.1);
    }

    private Point point(double x, double y) {
        return new Point(ImmutableList.of(x, y));
    }

    private AttributeName name(String name) {
        return AttributeNameImpl.of(name);
    }
}
