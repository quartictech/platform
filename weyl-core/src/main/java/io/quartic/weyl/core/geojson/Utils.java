package io.quartic.weyl.core.geojson;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.*;
import io.quartic.geojson.*;
import io.quartic.geojson.Geometry;
import io.quartic.geojson.LineString;
import io.quartic.geojson.MultiLineString;
import io.quartic.geojson.MultiPoint;
import io.quartic.geojson.MultiPolygon;
import io.quartic.geojson.Point;
import io.quartic.geojson.Polygon;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

public final class Utils {
    private static final GeometryFactory factory = new GeometryFactory();

    private Utils() {}

    public static com.vividsolutions.jts.geom.Geometry toJts(Geometry geometry) {
        return geometry.accept(new GeometryVisitor<com.vividsolutions.jts.geom.Geometry>(){
            @Override
            public com.vividsolutions.jts.geom.Geometry visit(Polygon polygon) {
                return polygonToJts(polygon);
            }

            @Override
            public com.vividsolutions.jts.geom.Geometry visit(Point point) {
                return pointToJts(point);
            }

            @Override
            public com.vividsolutions.jts.geom.Geometry visit(LineString lineString) {
                return lineStringToJts(lineString);
            }

            @Override
            public com.vividsolutions.jts.geom.Geometry visit(MultiLineString multiLineString) {
                return multiLineStringToJts(multiLineString);
            }

            @Override
            public com.vividsolutions.jts.geom.Geometry visit(MultiPoint multiPoint) {
                return multiPointToJts(multiPoint);
            }

            @Override
            public com.vividsolutions.jts.geom.Geometry visit(MultiPolygon multiPolygon) {
                return multiPolygonToJts(multiPolygon);
            }
        });
    }

    private static com.vividsolutions.jts.geom.MultiPoint multiPointToJts(MultiPoint multiPoint) {
        return factory.createMultiPoint(listToCoords(multiPoint.coordinates()));
    }

    private static com.vividsolutions.jts.geom.MultiLineString multiLineStringToJts(MultiLineString multiLineString) {
        return factory.createMultiLineString(multiLineString.coordinates().stream()
                .map(coords -> factory.createLineString(listToCoords(coords)))
                .toArray(com.vividsolutions.jts.geom.LineString[]::new));
    }

    private static com.vividsolutions.jts.geom.Point pointToJts(Point point) {
        return factory.createPoint(listToCoord(point.coordinates()));
    }

    private static com.vividsolutions.jts.geom.LineString lineStringToJts(LineString string) {
        return factory.createLineString(listToCoords(string.coordinates()));
    }

    private static com.vividsolutions.jts.geom.Polygon polygonToJts(Polygon polygon) {
        return createPolygon(polygon.coordinates());
    }

    private static com.vividsolutions.jts.geom.MultiPolygon multiPolygonToJts(MultiPolygon multiPolygon) {
        com.vividsolutions.jts.geom.Polygon[] polygons = multiPolygon.coordinates().stream().map(Utils::createPolygon)
                .toArray(com.vividsolutions.jts.geom.Polygon[]::new);
        return factory.createMultiPolygon(polygons);
    }

    private static com.vividsolutions.jts.geom.Polygon createPolygon(List<List<List<Double>>> coordinates) {
         LinearRing exterior = factory.createLinearRing(listToCoords(coordinates.get(0)));
            LinearRing[] holes = coordinates.stream().skip(1)
                    .map(Utils::listToCoords)
                    .map(factory::createLinearRing)
                    .toArray(LinearRing[]::new);

            return factory.createPolygon(exterior, holes);
    }

    public static Geometry fromJts(com.vividsolutions.jts.geom.Geometry geometry) {
        // TODO: this is gross - can we use a visitor?
        // TODO: the set of geometry types support on import (above) are not currently all supported here!
        if (geometry instanceof com.vividsolutions.jts.geom.Point) {
            return fromJts((com.vividsolutions.jts.geom.Point)geometry);
        }
        if (geometry instanceof com.vividsolutions.jts.geom.LineString) {
            return fromJts((com.vividsolutions.jts.geom.LineString)geometry);
        }
        if (geometry instanceof com.vividsolutions.jts.geom.Polygon) {
            return fromJts((com.vividsolutions.jts.geom.Polygon)geometry);
        }
        if (geometry instanceof com.vividsolutions.jts.geom.MultiPolygon) {
            return fromJts((com.vividsolutions.jts.geom.MultiPolygon)geometry);
        }

        throw new UnsupportedOperationException("Cannot convert from type " + geometry.getClass().getCanonicalName());
    }

    public static Point fromJts(com.vividsolutions.jts.geom.Point point) {
        return PointImpl.of(coordToList(point.getCoordinate()));
    }

    public static LineString fromJts(com.vividsolutions.jts.geom.LineString string) {
        return LineStringImpl.of(coordsToList(string.getCoordinates()));
    }

    public static Polygon fromJts(com.vividsolutions.jts.geom.Polygon polygon) {
        return PolygonImpl.of(polygonToList(polygon));
    }

    public static MultiPolygon fromJts(com.vividsolutions.jts.geom.MultiPolygon multiPolygon) {
        return MultiPolygonImpl.of(IntStream.range(0, multiPolygon.getNumGeometries())
                .mapToObj(i -> (com.vividsolutions.jts.geom.Polygon) multiPolygon.getGeometryN(i))
                .map(Utils::polygonToList)
                .collect(toList()));
    }

    private static List<List<List<Double>>> polygonToList(com.vividsolutions.jts.geom.Polygon polygon) {
        final List<List<List<Double>>> coords = Lists.newArrayList();
        coords.add(coordsToList(polygon.getExteriorRing().getCoordinates()));
        return IntStream.range(0, polygon.getNumInteriorRing())
                .mapToObj(i -> coordsToList(polygon.getInteriorRingN(i).getCoordinates()))
                .collect(toCollection(() -> coords));
    }

    private static Coordinate[] listToCoords(List<List<Double>> list) {
        return list.stream()
                .map(Utils::listToCoord)
                .collect(toList())
                .toArray(new Coordinate[0]);
    }

    private static Coordinate listToCoord(List<Double> list) {
        return new Coordinate(list.get(0), list.get(1));
    }

    private static List<List<Double>> coordsToList(Coordinate[] coordinates) {
        return Arrays.stream(coordinates)
                .map(Utils::coordToList)
                .collect(toList());
    }

    private static List<Double> coordToList(Coordinate coordinate) {
        return ImmutableList.of(coordinate.x, coordinate.y);
    }
}