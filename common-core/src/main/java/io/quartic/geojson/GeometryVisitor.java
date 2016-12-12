package io.quartic.geojson;

public interface GeometryVisitor<T> {
    T visit(Polygon polygon);
    T visit(Point point);
    T visit(LineString lineString);

    T visit(MultiLineString multiLineString);
    T visit(MultiPoint multiPoint);
    T visit(MultiPolygon multiPolygon);

    default T visit(Geometry geometry) {
        throw new UnsupportedOperationException("Cannot process type " + geometry.getClass().getCanonicalName());
    }
}
