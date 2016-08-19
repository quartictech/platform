package io.quartic.weyl.core.render;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class Mercator {
    public static Coordinate ul(int z, int x, int y){
        double n = Math.pow(2.0, z);
        double lon_deg = (x / n * 360.0) - 180.0;
        double lat_rad = Math.atan(Math.sinh(Math.PI * (1 - 2 * y / n)));
        double lat_deg = Math.toDegrees(lat_rad);

        return new Coordinate(lon_deg, lat_deg);
    }

    public static Envelope bounds(int z, int x, int y) {
        Coordinate a = ul(z, x, y);
        Coordinate b = ul(z, x + 1, y + 1);
        return new Envelope(a, b);
    }

    public static Coordinate xy(Coordinate c) {
        double x = 6378137.0 * Math.toRadians(c.x);
        double y = 6378137.0 * Math.log(
                Math.tan((Math.PI * 0.25) + (0.5 * Math.toRadians(c.y))));
        return new Coordinate(x, y);
    }
}
