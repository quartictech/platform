package io.quartic.weyl.core.utils;

import com.vividsolutions.jts.geom.Geometry;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.util.Optional;

public class GeometryTransformer {
    private final MathTransform transform;

    private GeometryTransformer(MathTransform transform) {
        this.transform = transform;
    }

    // for testing
    public static GeometryTransformer webMercatorToWebMercator() {
        return new GeometryTransformer(findMathTransform(webMercator(), webMercator()));
    }

    public static GeometryTransformer wgs84toWebMercator() {
        return new GeometryTransformer(findMathTransform(wgs84(), webMercator()));
    }

    public static GeometryTransformer webMercatortoWgs84() {
        return new GeometryTransformer(findMathTransform(webMercator(), wgs84()));
    }

    private static CoordinateReferenceSystem decodeCRS(String crs) {
         try {
            return CRS.decode(crs);
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }
    }

    private static CoordinateReferenceSystem wgs84() {
         // Ugh. See http://docs.geotools.org/latest/userguide/library/referencing/order.html
        CRSAuthorityFactory factory = CRS.getAuthorityFactory(true);
        try {
            return factory.createCoordinateReferenceSystem("EPSG:4326");
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }
    }

    private static CoordinateReferenceSystem webMercator() {
        return decodeCRS("EPSG:3857");
    }

    private static MathTransform findMathTransform(CoordinateReferenceSystem source, CoordinateReferenceSystem dest) {
        try {
            return CRS.findMathTransform(source, dest);
        } catch (FactoryException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Geometry> transform(Geometry geometry) {
        try {
            return Optional.ofNullable(JTS.transform(geometry, transform));
        } catch (TransformException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
