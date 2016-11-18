package io.quartic.weyl.core.compute;

import io.quartic.weyl.core.attributes.ComplexAttribute;
import io.quartic.weyl.core.model.AbstractFeature;
import io.quartic.weyl.core.model.AttributeName;

import java.util.Collection;
import java.util.Map;

import static java.util.stream.Collectors.*;

public class HistogramCalculator {
    public Collection<AbstractHistogram> calculate(Collection<? extends AbstractFeature> features) {
        final Map<AttributeName, Map<Object, Long>> counts = features.stream()
                .flatMap(f -> f.attributes().attributes().entrySet().stream())
                .filter(entry -> ! (entry.getValue() instanceof ComplexAttribute))
                .collect(groupingBy(Map.Entry::getKey,
                        groupingBy(Map.Entry::getValue, counting())));

        return counts.entrySet().stream()
                .map(e -> Histogram.of(e.getKey(), countsToBuckets(e.getValue())))
                .collect(toSet());
    }

    private Collection<Bucket> countsToBuckets(Map<Object, Long> counts) {
        return counts.entrySet().stream()
                .map(e2 -> Bucket.of(e2.getKey(), e2.getValue()))
                .collect(toSet());
    }
}
