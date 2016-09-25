package io.quartic.weyl.core.live;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import io.quartic.weyl.core.model.Feature;
import io.quartic.weyl.core.model.ImmutableFeature;

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Iterables.getLast;

class FeatureCache extends AbstractCollection<Feature<Geometry>> {
    private final Multimap<String, Feature<Geometry>> features = ArrayListMultimap.create();
    private final GeometryFactory factory = new GeometryFactory();

    @Override
    public synchronized Iterator<Feature<Geometry>> iterator() {
        return features.asMap().entrySet()
                .stream()
                .flatMap(e -> lineAndPointFromHistory((List<Feature<Geometry>>)e.getValue()))
                .collect(Collectors.toList())   // We make an explicit copy to avoid concurrency issues
                .iterator();
    }

    private Stream<Feature<Geometry>> lineAndPointFromHistory(List<Feature<Geometry>> history) {
        final Feature<Geometry> last = getLast(history);
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
    public synchronized int size() {
        return features.keySet().size();
    }

    @Override
    public synchronized boolean add(Feature<Geometry> feature) {
        return features.put(feature.id(), feature);
    }
}
