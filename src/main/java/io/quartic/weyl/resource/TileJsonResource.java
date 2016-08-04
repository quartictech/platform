package io.quartic.weyl.resource;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.quartic.weyl.GeoQueryConfig;
import org.skife.jdbi.v2.DBI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.List;
import java.util.Map;

@Path("/tilejson")
public class TileJsonResource {
    private final Map<String, GeoQueryConfig> queries;

    public TileJsonResource(Map<String, GeoQueryConfig> queries) {
        this.queries = queries;
    }

    @GET
    @Produces("application/json")
    public Map<String, Object> tileJson() {
        List<Map<String, String>> vectorLayers = Lists.newArrayListWithCapacity(queries.size());

        List<String> tileUrls = Lists.newArrayListWithCapacity(queries.size());
        for(Map.Entry<String, GeoQueryConfig> query : queries.entrySet()) {
            Map<String, String> properties = Maps.newHashMap();
            properties.put("description", query.getKey());
            properties.put("id", query.getKey());
            vectorLayers.add(properties);

            tileUrls.add("http://localhost:8080/api/" + query.getKey() + "/{z}/{x}/{y}.pbf");
        }

        Map<String, Object> output = Maps.newHashMap();
        output.put("tilejson", "2.1.0");
        output.put("name", "qt-geo");
        output.put("description", "Java based vector tile server");
        output.put("scheme", "xyz");
        output.put("format", "pbf");
        output.put("tiles", tileUrls);
        output.put("vector_layers", vectorLayers);

        return output;
    }
}
