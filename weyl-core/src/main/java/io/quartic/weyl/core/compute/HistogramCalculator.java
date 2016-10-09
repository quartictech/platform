package io.quartic.weyl.core.compute;

import io.quartic.weyl.core.model.Feature;

import java.util.Collection;
import java.util.Map;

import static java.util.stream.Collectors.*;

public class HistogramCalculator {
    public Collection<AbstractHistogram> calculate(Collection<? extends Feature> features) {
        final Map<String, Map<Object, Long>> counts = features.stream()
                .flatMap(f -> f.metadata().entrySet().stream())
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
