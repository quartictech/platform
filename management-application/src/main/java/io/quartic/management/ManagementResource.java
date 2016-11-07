package io.quartic.management;

import io.quartic.catalogue.api.*;
import io.quartic.weyl.common.uid.RandomUidGenerator;
import io.quartic.weyl.common.uid.UidGenerator;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Optional;

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;

@Path("/")
public class ManagementResource {
    private final GcsConnector gcsConnector;
    private final CatalogueService catalogueService;
    private final UidGenerator<CloudStorageId> cloudStorageIdGenerator = RandomUidGenerator.of(CloudStorageId::of);
    private final UidGenerator<TerminationId> terminatorEndpointIdGenerator = RandomUidGenerator.of(TerminationId::of);

    public ManagementResource(CatalogueService catalogueService, GcsConnector gcsConnector) {
        this.catalogueService = catalogueService;
        this.gcsConnector = gcsConnector;
    }

    @PUT
    @Path("/dataset")
    @Consumes(MediaType.APPLICATION_JSON)
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
                        TerminatorDatasetLocator.of(terminatorEndpointIdGenerator.get())
                );
            }
        });
        return catalogueService.registerDataset(datasetConfig);
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
                .header(CONTENT_TYPE, f.contentType())
                .entity(f.inputStream())
                .build()).orElseThrow(NotFoundException::new);
    }
}
