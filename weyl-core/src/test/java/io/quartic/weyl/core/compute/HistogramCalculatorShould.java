package io.quartic.weyl.core.compute;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.vividsolutions.jts.geom.Geometry;
import io.quartic.weyl.core.model.AttributeName;
import io.quartic.weyl.core.model.AttributeNameImpl;
import io.quartic.weyl.core.model.EntityIdImpl;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureImpl;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class HistogramCalculatorShould {
    private static final AttributeName SPECIES = AttributeNameImpl.of("species");
    private static final AttributeName NAME = AttributeNameImpl.of("name");
    private final HistogramCalculator calculator = new HistogramCalculator();

    @Test
    public void count_distinct_values_for_attribute() throws Exception {
        List<Feature> features = newArrayList(
                feature(ImmutableMap.of(NAME, "Alice")),
                feature(ImmutableMap.of(NAME, "Bob")),
                feature(ImmutableMap.of(NAME, "Alice"))
        );

        assertThat(calculator.calculate(features),
                equalTo(ImmutableSet.of(
                    HistogramImpl.of(NAME, ImmutableSet.of(BucketImpl.of("Alice", 2L), BucketImpl.of("Bob", 1L)))
                )));
    }

    @Test
    public void count_distinct_values_for_multiple_attributes() throws Exception {
        List<Feature> features = newArrayList(
                feature(ImmutableMap.of(NAME, "Alice", SPECIES, "dog")),
                feature(ImmutableMap.of(NAME, "Bob", SPECIES, "dog")),
                feature(ImmutableMap.of(NAME, "Alice", SPECIES, "cat"))
        );

        assertThat(calculator.calculate(features),
                equalTo(ImmutableSet.of(
                        HistogramImpl.of(NAME, ImmutableSet.of(BucketImpl.of("Alice", 2L), BucketImpl.of("Bob", 1L))),
                        HistogramImpl.of(SPECIES, ImmutableSet.of(BucketImpl.of("dog", 2L), BucketImpl.of("cat", 1L)))
                )));
    }

    @Test
    public void handle_missing_attributes() throws Exception {
        List<Feature> features = newArrayList(
                feature(ImmutableMap.of(NAME, "Alice", SPECIES, "dog")),
                feature(ImmutableMap.of(NAME, "Bob")),
                feature(ImmutableMap.of(NAME, "Alice", SPECIES, "cat"))
        );

        assertThat(calculator.calculate(features),
                equalTo(ImmutableSet.of(
                        HistogramImpl.of(NAME, ImmutableSet.of(BucketImpl.of("Alice", 2L), BucketImpl.of("Bob", 1L))),
                        HistogramImpl.of(SPECIES, ImmutableSet.of(BucketImpl.of("dog", 1L), BucketImpl.of("cat", 1L)))
                )));
    }

    private Feature feature(Map<AttributeName, Object> attributes) {
        return FeatureImpl.builder()
                .entityId(EntityIdImpl.of("def"))
                .geometry(mock(Geometry.class))
                .attributes(() -> attributes)
                .build();
    }
}
