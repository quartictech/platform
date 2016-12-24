package io.quartic.geojson

interface GeometryVisitor<T> {
    fun visit(polygon: Polygon): T
    fun visit(point: Point): T
    fun visit(lineString: LineString): T

    fun visit(multiLineString: MultiLineString): T
    fun visit(multiPoint: MultiPoint): T
    fun visit(multiPolygon: MultiPolygon): T

    fun visit(geometry: Geometry): T {
        throw UnsupportedOperationException("Cannot process type " + geometry.javaClass.canonicalName)
    }
}
