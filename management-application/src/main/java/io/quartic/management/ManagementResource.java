package io.quartic.management;

import io.quartic.catalogue.api.*;
import io.quartic.common.uid.RandomUidGenerator;
import io.quartic.common.uid.UidGenerator;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyMap;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;

@Path("/")
public class ManagementResource {
    private final GcsConnector gcsConnector;
    private final CatalogueService catalogueService;
    private final UidGenerator<CloudStorageId> cloudStorageIdGenerator = RandomUidGenerator.of(CloudStorageIdImpl::of);
    private final UidGenerator<TerminationId> terminatorEndpointIdGenerator = RandomUidGenerator.of(TerminationIdImpl::of);

    public ManagementResource(CatalogueService catalogueService, GcsConnector gcsConnector) {
        this.catalogueService = catalogueService;
        this.gcsConnector = gcsConnector;
    }

    @GET
    @Path("/dataset")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<DatasetId, DatasetConfig> getDatasets() {
        return catalogueService.getDatasets();
    }

    @POST
    @Path("/dataset")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public DatasetId createDataset(CreateDatasetRequest createDatasetRequest) {
        DatasetConfig datasetConfig = createDatasetRequest.accept(new CreateDatasetRequest.Visitor<DatasetConfig>() {
            @Override
            public DatasetConfig visit(CreateStaticDatasetRequest request) {
                return DatasetConfigImpl.of(
                        request.metadata(),
                        CloudGeoJsonDatasetLocatorImpl.of("/file/" + request.fileName()),
                        emptyMap()
                );
            }

            @Override
            public DatasetConfig visit(CreateLiveDatasetRequest request) {
                return DatasetConfigImpl.of(
                        request.metadata(),
                        TerminatorDatasetLocatorImpl.of(terminatorEndpointIdGenerator.get()),
                        emptyMap()
                );
            }
        });
        return catalogueService.registerDataset(datasetConfig);
    }

    @POST
    @Path("/file")
    @Produces(MediaType.APPLICATION_JSON)
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
