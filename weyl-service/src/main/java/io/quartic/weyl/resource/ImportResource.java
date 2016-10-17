package io.quartic.weyl.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import io.quartic.weyl.core.LayerStore;
import io.quartic.weyl.core.feature.FeatureStore;
import io.quartic.weyl.core.importer.GeoJsonImporter;
import io.quartic.weyl.core.importer.PostgresImporter;
import io.quartic.weyl.core.model.LayerId;
import io.quartic.weyl.core.model.LayerMetadata;
import io.quartic.weyl.request.GeoJsonImportRequest;
import io.quartic.weyl.request.PostgresImportRequest;
import org.skife.jdbi.v2.DBI;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.function.Supplier;

@Path("/import")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ImportResource {
    private final LayerStore layerStore;
    private final Supplier<DBI> dbiSupplier;
    private final FeatureStore featureStore;
    private final ObjectMapper objectMapper;

    public ImportResource(LayerStore layerStore, Supplier<DBI> dbiSupplier, FeatureStore featureStore, ObjectMapper objectMapper) {
        this.layerStore = layerStore;
        this.dbiSupplier = dbiSupplier;
        this.featureStore = featureStore;
        this.objectMapper = objectMapper;
    }

    @PUT
    @Path("/postgres")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public LayerId importPostgres(PostgresImportRequest request) {
        PostgresImporter postgresImporter = PostgresImporter.fromDBI(dbiSupplier.get(), request.query(), featureStore, objectMapper);
        return layerStore.createAndImportToLayer(postgresImporter, request.metadata());
    }

    @PUT
    @Path("/geojson")
    public LayerId importGeoJson(GeoJsonImportRequest request) throws IOException {
        Preconditions.checkNotNull(request.name());
        Preconditions.checkNotNull(request.description());
        LayerMetadata metadata = LayerMetadata.builder()
                .name(request.name())
                .description(request.description())
                .build();
        GeoJsonImporter geoJsonImporter = GeoJsonImporter.fromObject(request.data(), featureStore, objectMapper);
        return layerStore.createAndImportToLayer(geoJsonImporter, metadata);
    }
}
