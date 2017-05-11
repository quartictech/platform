package io.quartic.weyl.core.geojson

import com.vividsolutions.jts.geom.Coordinate
import com.vividsolutions.jts.geom.GeometryFactory
import io.quartic.common.geojson.*

private val factory = GeometryFactory()

fun toJts(geometry: Geometry): com.vividsolutions.jts.geom.Geometry = when (geometry) {
    is Point -> factory.createPoint(listToCoord(geometry.coordinates))
    is LineString -> factory.createLineString(listToCoords(geometry.coordinates))
    is Polygon -> createPolygon(geometry.coordinates)
    is MultiPoint -> factory.createMultiPoint(listToCoords(geometry.coordinates))
    is MultiLineString -> factory.createMultiLineString(geometry.coordinates
            .map({ coords -> factory.createLineString(listToCoords(coords)) })
            .toTypedArray())
    is MultiPolygon -> factory.createMultiPolygon(geometry.coordinates
            .map(::createPolygon)
            .toTypedArray())
    is GeometryCollection -> factory
                .createGeometryCollection(geometry.geometries.map(::toJts).toTypedArray())
    else -> throw RuntimeException("Unsupported geometry type: ${geometry.javaClass.canonicalName}")
}

private fun createPolygon(coordinates: List<List<List<Double>>>): com.vividsolutions.jts.geom.Polygon {
    val exterior = factory.createLinearRing(listToCoords(coordinates.first()))
    val holes = coordinates
            .drop(1)
            .map(::listToCoords)
            .map({ factory.createLinearRing(it) })
            .toTypedArray()
    return factory.createPolygon(exterior, holes)
}

// TODO: the set of geometry types support on import (above) are not currently all supported here!
fun fromJts(geometry: com.vividsolutions.jts.geom.Geometry) = when (geometry) {
    is com.vividsolutions.jts.geom.Point -> fromJts(geometry)
    is com.vividsolutions.jts.geom.LineString -> fromJts(geometry)
    is com.vividsolutions.jts.geom.Polygon -> fromJts(geometry)
    is com.vividsolutions.jts.geom.MultiPolygon -> fromJts(geometry)
    else -> throw UnsupportedOperationException("Unsupported geometry type: ${geometry.javaClass.canonicalName}")
}

fun fromJts(point: com.vividsolutions.jts.geom.Point): Point {
    return Point(coordToList(point.coordinate))
}

fun fromJts(string: com.vividsolutions.jts.geom.LineString): LineString {
    return LineString(coordsToList(string.coordinates))
}

fun fromJts(polygon: com.vividsolutions.jts.geom.Polygon): Polygon {
    return Polygon(polygonToList(polygon))
}

fun fromJts(multiPolygon: com.vividsolutions.jts.geom.MultiPolygon): MultiPolygon {
    return MultiPolygon((0 until multiPolygon.numGeometries)
            .map({ i -> multiPolygon.getGeometryN(i) as com.vividsolutions.jts.geom.Polygon })
            .map(::polygonToList))
}

private fun polygonToList(polygon: com.vividsolutions.jts.geom.Polygon): List<List<List<Double>>> {
    val coords = mutableListOf<List<List<Double>>>()
    coords.add(coordsToList(polygon.exteriorRing.coordinates))
    coords.addAll((0 until polygon.numInteriorRing)
            .map({ i -> coordsToList(polygon.getInteriorRingN(i).coordinates) }))
    return coords
}

private fun listToCoords(list: List<List<Double>>) = list.map(::listToCoord).toTypedArray()

private fun listToCoord(list: List<Double>) = Coordinate(list[0], list[1])

private fun coordsToList(coordinates: Array<Coordinate>) = coordinates.map(::coordToList)

private fun coordToList(coordinate: Coordinate) = listOf(coordinate.x, coordinate.y)