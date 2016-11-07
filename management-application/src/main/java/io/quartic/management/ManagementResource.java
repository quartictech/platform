package io.quartic.management;

import io.quartic.catalogue.api.*;
import io.quartic.weyl.common.uid.RandomUidGenerator;
import io.quartic.weyl.common.uid.UidGenerator;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Optional;

@Path("/")
public class ManagementResource {
    private final GcsConnector gcsConnector;
    private final CatalogueService catalogueService;
    private final UidGenerator<CloudStorageId> cloudStorageIdGenerator = RandomUidGenerator.of(CloudStorageId::of);
    private final UidGenerator<TerminatorEndpointId> terminatorEndpointIdGenerator = RandomUidGenerator.of(TerminatorEndpointId::of);

    public ManagementResource(CatalogueService catalogueService, GcsConnector gcsConnector) {
        this.catalogueService = catalogueService;
        this.gcsConnector = gcsConnector;
    }

    @PUT
    @Consumes("application/json")
    @Path("/dataset")
    public DatasetId createDataset(CreateDatasetRequest createDatasetRequest) {
        DatasetConfig datasetConfig = createDatasetRequest.accept(new CreateDatasetRequest.Visitor<DatasetConfig>() {
                    @Override
                    public DatasetConfig visit(AbstractCreateStaticDatasetRequest request) {
                        return DatasetConfig.of(
                                request.metadata(),
                                CloudGeoJsonDatasetLocator.of("/file/" + request.fileName())
                        );
                    }

                    @Override
                    public DatasetConfig visit(AbstractCreateLiveDatasetRequest request) {
                        return DatasetConfig.of(
                                request.metadata(),
                                TerminatorDatasetLocator.of("/api/" + terminatorEndpointIdGenerator.get().uid())
                        );
                    }
        });
        DatasetId datasetId = catalogueService.registerDataset(datasetConfig);
        return datasetId;
    }

    @POST
    @Path("/file")
    public CloudStorageId uploadFile(@Context HttpServletRequest request) throws IOException {
        CloudStorageId cloudStorageId = cloudStorageIdGenerator.get();
        gcsConnector.put(request.getContentType(), cloudStorageId.uid(), request.getInputStream());
        return cloudStorageId;
    }

    @GET
    @Path("/file/{fileName}")
    public Response download(@PathParam("fileName") String fileName) throws IOException {
        Optional<InputStreamWithContentType> file = gcsConnector.get(fileName);

        return file.map( f ->
            Response.ok()
                .header("Content-Type", f.contentType())
                .entity(f.inputStream())
                .build()).orElseThrow(NotFoundException::new);
    }
}
