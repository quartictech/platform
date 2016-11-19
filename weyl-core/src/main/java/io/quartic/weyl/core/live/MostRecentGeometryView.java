package io.quartic.weyl.core.live;

import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.EntityId;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.google.common.collect.Iterables.getLast;
import static java.util.stream.Collectors.groupingBy;

public class MostRecentGeometryView implements LayerView {
    @Override
    public Stream<Feature> compute(Collection<Feature> history) {
        Map<EntityId, List<Feature>> historyById = history.stream()
                .collect(groupingBy(Feature::entityId));

        return historyById.entrySet().stream().map(entry -> getLast(entry.getValue()));
    }
}
