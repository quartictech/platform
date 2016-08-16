package io.quartic.weyl.resource;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;
import io.dropwizard.jersey.caching.CacheControl;
import io.quartic.weyl.GeoQueryConfig;
import io.quartic.weyl.util.DataCache;
import io.quartic.weyl.util.Mercator;
import no.ecc.vectortile.VectorTileEncoder;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Path("/")
public class TileResource {
    private static final Logger log = LoggerFactory.getLogger(TileResource.class);
    private static final String GEOM_FIELD = "geom_wkb";
    private final DBI dbi;
    private final Map<String, GeoQueryConfig> queries;
    private final DataCache cache;

    public TileResource(DBI dbi, Map<String, GeoQueryConfig> queries, DataCache cache) {
        this.dbi = dbi;
        this.queries = queries;
        this.cache = cache;
    }

    @GET
    @Produces("application/protobuf")
    @Path("/{query}/{z}/{x}/{y}.pbf")
    @CacheControl(maxAge = 60*60)
    public byte[] protobuf(@PathParam("query") String queryName,
                           @PathParam("z") Integer z,
                           @PathParam("x") Integer x,
                           @PathParam("y") Integer y) throws ParseException, IOException {

        Optional<byte[]> cachedData = cache.getTile(queryName, z, x, y);
        if (cachedData.isPresent()) {
            log.info("Hitting cache for query");
            return cachedData.get();
        }

        Envelope bounds = Mercator.bounds(z, x, y);
        log.info("Bounds: {}", bounds);
        Coordinate southWest = Mercator.xy(new Coordinate(bounds.getMinX(), bounds.getMinY()));
        Coordinate northEast = Mercator.xy(new Coordinate(bounds.getMaxX(), bounds.getMaxY()));

        log.info("South west: {}, North east: {}", southWest, northEast);

        if (! queries.containsKey(queryName))  {
            throw new NotFoundException("No query called: " + queryName);
        }

        GeoQueryConfig queryConfig = queries.get(queryName);
        Handle h = dbi.open();

        String scale_box = String.format("%.12f, %.12f, %.12f, %.12f", -southWest.x, -southWest.y,
                4096.0 / (northEast.x - southWest.x),
                4096.0 / (northEast.y - southWest.y));
        String bounding_box = String.format("ST_SetSRID(ST_MakeBox2d(ST_MakePoint(%.12f, %.12f), ST_MakePoint(%.12f, %.12f)), 900913)",
                southWest.x, southWest.y, northEast.x, northEast.y);
        String sql = String.format("SELECT ST_AsBinary(ST_TransScale(geom, %s)) as geom_wkb, * " +
                "FROM (%s) AS data WHERE" +
                " ST_IsValid(geom) AND ST_Intersects(geom, %s)", scale_box, queryConfig.getSql(),
                bounding_box);
        log.info("Query: {}", sql);
        Query<Map<String, Object>> query = h.createQuery(sql);


        VectorTileEncoder encoder = new VectorTileEncoder(4096, 8, false);
        WKBReader wkbReader = new WKBReader();
        long time = System.currentTimeMillis();
        List<Map<String, Object>> results = query.list();
        log.info("Found {} results in {} ms", results.size(), System.currentTimeMillis() - time);

        //String sql2 = String.format("SELECT ST_AsGeoJson(ST_TransScale(geom, %s)) as geom_geojson, * " +
        //        "FROM (%s) AS data WHERE" +
        //        " ST_IsValid(geom) AND ST_Intersects(geom, %s)", scale_box, queryConfig.getSql(),
        //        bounding_box);
        //Query<Map<String, Object>> queryGeoJson = h.createQuery(sql2);
        //for (Map<String, Object> row : queryGeoJson.list()) {
        //    log.info("Geojson: {}", row.get("geom_geojson"));
        //}


        h.close();
        int size = 0;
        for (Map<String, Object> row : results) {
            byte[] wkb = (byte[]) row.get("geom_wkb");
            size += wkb.length;

            if (wkb == null) {
               throw new IOException("Unable to find field with name " + GEOM_FIELD);
            }

            Geometry geom = wkbReader.read(wkb);
            geom.apply(new CoordinateFilter() {
                           @Override
                           public void filter(Coordinate coord) {
                               coord.y = 4096.0 - coord.y;
                           }
                       }
            );
            geom.geometryChanged();

            TopologyPreservingSimplifier simplifier = new TopologyPreservingSimplifier(geom);
            simplifier.setDistanceTolerance(4096.0 / 1000);

            //log.info("Geometry: {}", geom);

            Map<String, Object> attributes = new HashMap<>(row);
            attributes.remove("geom_wkb");
            attributes.remove("geom");

            encoder.addFeature(queryName, attributes, simplifier.getResultGeometry());
        }

        log.info("Binary size: {}", size);

        byte[] data =  encoder.encode();
        cache.putTile(queryName, z, x, y, data);
        return data;
    }
}
