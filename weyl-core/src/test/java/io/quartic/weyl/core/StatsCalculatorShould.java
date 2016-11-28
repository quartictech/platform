package io.quartic.weyl.core;

import com.google.common.collect.ImmutableMap;
import io.quartic.weyl.core.feature.FeatureCollection;
import io.quartic.weyl.core.model.*;
import org.junit.Test;

import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static io.quartic.common.CollectionUtils.entry;
import static io.quartic.common.CollectionUtils.map;
import static io.quartic.weyl.core.feature.FeatureCollection.EMPTY_COLLECTION;
import static io.quartic.weyl.core.model.AttributeType.NUMERIC;
import static io.quartic.weyl.core.model.AttributeType.STRING;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StatsCalculatorShould {

    private static final AttributeName HEIGHT = mock(AttributeName.class);
    private static final AttributeName WEIGHT = mock(AttributeName.class);
    private static final AttributeName NAME = mock(AttributeName.class);

    @Test
    public void track_min_and_max_of_numeric_attributes() throws Exception {
        final Map<AttributeName, Attribute> attributes = map(
                entry(HEIGHT, attribute(NUMERIC)),
                entry(WEIGHT, attribute(NUMERIC))
        );

        final FeatureCollection features = EMPTY_COLLECTION.append(newArrayList(
                feature(map(entry(WEIGHT, 70.0), entry(HEIGHT, 120.0))),
                feature(map(entry(WEIGHT, 50.0), entry(HEIGHT, 140.0))),
                feature(map(entry(WEIGHT, 60.0), entry(HEIGHT, 130.0)))
        ));

        final LayerStats stats = calculate(attributes, features);

        assertThat(stats, equalTo(LayerStatsImpl.of(map(
                entry(HEIGHT, AttributeStatsImpl.of(120.0, 140.0)),
                entry(WEIGHT, AttributeStatsImpl.of(50.0, 70.0))
                ),
                3
        )));
    }

    @Test
    public void cope_with_null_attributes() throws Exception {
        final Map<AttributeName, Attribute> attributes = map(
                entry(HEIGHT, attribute(NUMERIC)),
                entry(WEIGHT, attribute(NUMERIC))
        );

        final FeatureCollection features = EMPTY_COLLECTION.append(newArrayList(
                feature(map(entry(WEIGHT, 70.0), entry(HEIGHT, 120.0))),
                feature(map(entry(WEIGHT, 50.0), entry(HEIGHT, null))),                 // Height is missing
                feature(map(entry(WEIGHT, 60.0), entry(HEIGHT, 130.0)))
        ));

        assertThat(calculate(attributes, features), equalTo(LayerStatsImpl.of(map(
                entry(HEIGHT, AttributeStatsImpl.of(120.0, 130.0)),
                entry(WEIGHT, AttributeStatsImpl.of(50.0, 70.0))
                ),
                3
        )));
    }

    @Test
    public void cope_if_all_features_missing_an_attribute() throws Exception {
        final Map<AttributeName, Attribute> attributes = ImmutableMap.of(
                HEIGHT, attribute(NUMERIC),
                WEIGHT, attribute(NUMERIC)
        );

        final FeatureCollection features = EMPTY_COLLECTION.append(newArrayList(
                feature(map(entry(WEIGHT, 70.0))),
                feature(map(entry(WEIGHT, 50.0))),
                feature(map(entry(WEIGHT, 60.0)))
        ));

        assertThat(calculate(attributes, features), equalTo(LayerStatsImpl.of(map(
                entry(WEIGHT, AttributeStatsImpl.of(50.0, 70.0))
                ),
                3
        )));
    }

    @Test
    public void ignore_non_numeric_attributes() throws Exception {
        final Map<AttributeName, Attribute> attributes = ImmutableMap.of(
                NAME, attribute(STRING),
                WEIGHT, attribute(NUMERIC)
        );

        final FeatureCollection features = EMPTY_COLLECTION.append(newArrayList(
                feature(map(entry(NAME, "Alice"), entry(WEIGHT, 70.0))),
                feature(map(entry(NAME, "Bob"), entry(WEIGHT, 50.0))),
                feature(map(entry(NAME, "Charles"), entry(WEIGHT, 60.0)))
        ));

        assertThat(calculate(attributes, features), equalTo(LayerStatsImpl.of(map(
                entry(WEIGHT, AttributeStatsImpl.of(50.0, 70.0))
                ),
                3
        )));
    }

    private LayerStats calculate(Map<AttributeName, Attribute> attributes, FeatureCollection features) {
        final AttributeSchema schema = mock(AttributeSchema.class);
        when(schema.attributes()).thenReturn(attributes);

        return StatsCalculator.calculateStats(schema, features);
    }

    private Feature feature(Map<AttributeName, Object> attributes) {
        final Feature feature = mock(Feature.class);
        when(feature.attributes()).thenReturn(() -> attributes);
        return feature;
    }

    private Attribute attribute(AttributeType type) {
        final Attribute attribute = mock(Attribute.class);
        when(attribute.type()).thenReturn(type);
        return attribute;
    }
}
