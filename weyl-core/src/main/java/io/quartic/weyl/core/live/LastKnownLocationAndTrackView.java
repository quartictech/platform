package io.quartic.weyl.core.live;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.FeatureId;
import io.quartic.weyl.core.model.ImmutableFeature;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Iterables.getLast;

public class LastKnownLocationAndTrackView implements LiveLayerView {

    private static Stream<Feature> makeTrack(List<Feature> history) {
        final Feature last = getLast(history);
        final GeometryFactory factory = last.geometry().getFactory();
        if (history.size() == 1) {
            return Stream.of(last);
        }
        return Stream.of(
                last,
                ImmutableFeature.of(
                        last.id(),
                        factory.createLineString(
                                history.stream()
                                        .map(f -> f.geometry().getCoordinate())
                                        .collect(Collectors.toList())
                                        .toArray(new Coordinate[0])
                        ),
                        last.metadata())
        );
    }

    @Override
    public Stream<Feature> compute(Collection<Feature> history) {
        Map<FeatureId, List<Feature>> historyById = history.stream()
                .collect(Collectors.groupingBy(Feature::id));

        return historyById.values()
                .stream()
                .flatMap(LastKnownLocationAndTrackView::makeTrack);
    }
}
