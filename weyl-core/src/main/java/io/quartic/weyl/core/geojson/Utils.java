package io.quartic.weyl.core.geojson;

import com.google.common.collect.ImmutableList;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Arrays.asList;

public final class Utils {
    private static final GeometryFactory factory = new GeometryFactory();

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

    public static com.vividsolutions.jts.geom.Geometry toJts(Geometry geometry) {
        if (geometry instanceof Point) {
            Point point = (Point)geometry;
            return factory.createPoint(listToCoord(point.coordinates()));
            // TODO
        }if (geometry instanceof LineString) {
            LineString string = (LineString)geometry;
            return factory.createLineString(listToCoords(string.coordinates()));
        }
        throw new UnsupportedOperationException("Cannot convert from type " + geometry.getClass().getCanonicalName());
    }

    public static Geometry fromJts(com.vividsolutions.jts.geom.Geometry geometry) {
        // TODO: this is gross - can we use a visitor?
        if (geometry instanceof com.vividsolutions.jts.geom.Point) {
            com.vividsolutions.jts.geom.Point point = (com.vividsolutions.jts.geom.Point)geometry;
            return Point.of(coordToList(point.getCoordinate()));
        }
        if (geometry instanceof com.vividsolutions.jts.geom.LineString) {
            com.vividsolutions.jts.geom.LineString string = (com.vividsolutions.jts.geom.LineString)geometry;
            return LineString.of(coordsToList(string.getCoordinates()));
        }
        throw new UnsupportedOperationException("Cannot convert from type " + geometry.getClass().getCanonicalName());
    }

    private static Coordinate[] listToCoords(List<List<Double>> list) {
        return list.stream()
                .map(Utils::listToCoord)
                .collect(Collectors.toList())
                .toArray(new Coordinate[0]);
    }

    private static Coordinate listToCoord(List<Double> list) {
        return new Coordinate(list.get(0), list.get(1));
    }

    private static List<List<Double>> coordsToList(Coordinate[] coordinates) {
        return Arrays.stream(coordinates)
                .map(Utils::coordToList)
                .collect(Collectors.toList());
    }

    private static List<Double> coordToList(Coordinate coordinate) {
        return ImmutableList.of(coordinate.x, coordinate.y);
    }
}
