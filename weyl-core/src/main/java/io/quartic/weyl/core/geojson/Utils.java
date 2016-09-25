package io.quartic.weyl.core.geojson;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Arrays.asList;

public final class Utils {
    private Utils() {}

    public static LineString lineStringFrom(Point... points) {
        return lineStringFrom(asList(points));
    }

    public static LineString lineStringFrom(Iterable<Point> points) {
        return LineString.of(
                StreamSupport.stream(points.spliterator(), true)
                        .map(Point::coordinates)
                        .collect(Collectors.toList())
        );
    }
}
