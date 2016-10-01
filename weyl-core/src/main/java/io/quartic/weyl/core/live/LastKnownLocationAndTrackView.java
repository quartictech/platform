package io.quartic.weyl.core.live;

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.ImmutableFeature;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Iterables.getLast;

public class LastKnownLocationAndTrackView implements LiveLayerView {
    @Override
    public Stream<Feature> compute(Collection<Feature> history) {
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
}
