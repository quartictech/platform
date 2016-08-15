package io.quartic.weyl.resource;

import com.google.common.collect.Lists;
import io.quartic.weyl.GeoQueryConfig;
import io.quartic.weyl.util.DataCache;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Path("/geojson")
public class GeoJsonResource {
    private static final Logger log = LoggerFactory.getLogger(TileResource.class);
    private final Map<String, GeoQueryConfig> queries;
    private final DBI dbi;
    private final DataCache cache;

    public GeoJsonResource(DBI dbi, Map<String, GeoQueryConfig> queries, DataCache cache) {
        this.dbi = dbi;
        this.queries = queries;
        this.cache = cache;
    }

    @GET
    @Path("/{query}.geojson")
    @Produces("application/json")
    public String geoJson(@PathParam("query") String queryName) {
        Optional<byte[]> cachedData = cache.getVector(queryName);
        if (cachedData.isPresent()) {
            log.info("Hitting cache for query");
            return new String(cachedData.get());
        }

        if (! queries.containsKey(queryName))  {
               throw new NotFoundException("No query called: " + queryName);
        }

        GeoQueryConfig queryConfig = queries.get(queryName);

        String sql = String.format("SELECT ST_AsGeoJson(ST_Transform(geom, 4326)) as geom_geojson, * " +
                "FROM (%s) AS data WHERE" +
                " ST_IsValid(geom)", queryConfig.getSql());
        log.info("Query: {}", sql);
        Handle h = dbi.open();
        Query<Map<String, Object>> query = h.createQuery(sql);

        List<Map<String, Object>> results = query.list();

        StringBuilder sb = new StringBuilder();

        sb.append(" { \"type\": \"FeatureCollection\", \"features\": [");

        List<String> featureJsons = Lists.newArrayListWithCapacity(results.size());
        for (Map<String, Object> row : results) {
            String featureJson = String.format("{ \"type\": \"Feature\", \"geometry\": %s, \"properties\": {} }",
                    row.get("geom_geojson"));
            featureJsons.add(featureJson);
        }

        sb.append(String.join(",", featureJsons));

        sb.append("]}");

        String result = sb.toString();
        cache.putVector(queryName, result.getBytes());
        return result;
    }
}
