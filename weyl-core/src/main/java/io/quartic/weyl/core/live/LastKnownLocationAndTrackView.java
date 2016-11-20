package io.quartic.weyl.core.live;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.weyl.core.model.EntityId;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureImpl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;

public class LastKnownLocationAndTrackView implements LayerView {

    @Override
    public Stream<Feature> compute(Collection<Feature> history) {
        Map<EntityId, List<Feature>> historyById = history.stream()
                .collect(groupingBy(Feature::entityId));

        return historyById.values()
                .stream()
                .flatMap(LastKnownLocationAndTrackView::makeTrack);
    }

    private static Stream<Feature> makeTrack(List<Feature> history) {
        final Feature first = history.get(0);
        final GeometryFactory factory = first.geometry().getFactory();
        if (history.size() == 1) {
            return Stream.of(first);
        }
        return Stream.of(
                first,
                FeatureImpl.builder()
                        .entityId(first.entityId())
                        .geometry(factory.createLineString(history.stream()
                                .map(f -> f.geometry().getCoordinate())
                                .collect(Collectors.toList())
                                .toArray(new Coordinate[0])
                        ))
                        .attributes(first.attributes())
                        .build()
        );
    }
}
