package io.quartic.weyl.core.feature;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.common.geojson.Feature;
import io.quartic.common.geojson.FeatureCollection;
import io.quartic.common.geojson.Geometry;
import io.quartic.common.geojson.Point;
import io.quartic.weyl.core.attributes.AttributesFactory;
import io.quartic.weyl.core.attributes.TimeSeriesAttributeImpl;
import io.quartic.weyl.core.feature.FeatureConverter.AttributeManipulator;
import io.quartic.weyl.core.model.AttributeImpl;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.AttributeNameImpl;
import io.quartic.weyl.core.model.Attributes;
import io.quartic.weyl.core.model.DynamicSchema;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.NakedFeature;
import io.quartic.weyl.core.model.NakedFeatureImpl;
import org.junit.Test;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static io.quartic.common.test.CollectionUtilsKt.entry;
import static io.quartic.common.test.CollectionUtilsKt.map;
import static io.quartic.weyl.core.feature.FeatureConverter.DEFAULT_MANIPULATOR;
import static io.quartic.weyl.core.model.AttributeType.NUMERIC;
import static io.quartic.weyl.core.model.AttributeType.STRING;
import static io.quartic.weyl.core.model.AttributeType.TIMESTAMP;
import static io.quartic.weyl.core.model.AttributeType.TIME_SERIES;
import static io.quartic.weyl.core.model.AttributeType.UNKNOWN;
import static io.quartic.weyl.core.utils.GeometryTransformer.webMercatorToWebMercator;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
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
    public void strip_only_nulls_when_creating_raw() throws Exception {
        assertThat(converter.toGeojson(DEFAULT_MANIPULATOR, heterogeneousFeature()).getProperties(), equalTo(map(
                entry("timestamp", 1234),
                entry("complex", TimeSeriesAttributeImpl.of(emptyList()))
        )));
    }

    @Test
    public void create_manipulator_that_only_accepts_categorical_and_numerical() throws Exception {
        final DynamicSchema schema = mock(DynamicSchema.class);
        final AttributeManipulator manipulator = FeatureConverter.frontendManipulatorFor(schema);

        when(schema.attributes()).thenReturn(map(
                entry(name("numeric"), AttributeImpl.of(NUMERIC, Optional.empty())),
                entry(name("string"), AttributeImpl.of(STRING, Optional.empty())),
                entry(name("timeseries"), AttributeImpl.of(TIME_SERIES, Optional.empty())),
                entry(name("timestamp"), AttributeImpl.of(TIMESTAMP, Optional.empty())),
                entry(name("unknown"), AttributeImpl.of(UNKNOWN, Optional.empty())),
                entry(name("categorical"), AttributeImpl.of(UNKNOWN, Optional.of(newHashSet(1, 2, 3))))
        ));

        assertTrue(manipulator.test(name("numeric"), null));
        assertFalse(manipulator.test(name("string"), null));
        assertFalse(manipulator.test(name("timeseries"), null));
        assertTrue(manipulator.test(name("timestamp"), null));
        assertFalse(manipulator.test(name("unknown"), null));
        assertTrue(manipulator.test(name("categorical"), null));
    }

    @Test
    public void create_manipulator_that_adds_ids() throws Exception {
        final AttributeManipulator manipulator = FeatureConverter.frontendManipulatorFor(mock(DynamicSchema.class));
        final io.quartic.weyl.core.model.Feature feature = mock(io.quartic.weyl.core.model.Feature.class, RETURNS_DEEP_STUBS);
        final Map<String, Object> attributes = newHashMap();
        when(feature.entityId()).thenReturn(EntityId.fromString("123"));

        manipulator.postProcess(feature, attributes);

        assertThat(attributes, hasEntry("_entityId", "123"));
    }

    private io.quartic.weyl.core.model.Feature heterogeneousFeature() {
        return io.quartic.weyl.core.model.FeatureImpl.of(
                EntityId.fromString("a"),
                factory.createPoint(new Coordinate(51.0, 0.1)),
                () -> map(
                        entry(name("timestamp"), 1234),
                        entry(name("noob"), null),
                        entry(name("complex"), TimeSeriesAttributeImpl.of(emptyList()))
                )
        );
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
