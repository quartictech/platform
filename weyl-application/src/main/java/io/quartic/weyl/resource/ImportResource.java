package io.quartic.weyl.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.importer.GeoJsonImporter;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.request.GeoJsonImportRequest;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

@Path("/import")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ImportResource {
    private final LayerStore layerStore;
    private final FeatureStore featureStore;
    private final ObjectMapper objectMapper;

    public ImportResource(LayerStore layerStore, FeatureStore featureStore, ObjectMapper objectMapper) {
        this.layerStore = layerStore;
        this.featureStore = featureStore;
        this.objectMapper = objectMapper;
    }

    @PUT
    @Path("/geojson")
    public LayerId importGeoJson(GeoJsonImportRequest request) throws IOException {
        GeoJsonImporter geoJsonImporter = GeoJsonImporter.fromObject(request.data(), featureStore, objectMapper);
        return layerStore.createAndImportToLayer(geoJsonImporter, request.metadata());
    }
}
