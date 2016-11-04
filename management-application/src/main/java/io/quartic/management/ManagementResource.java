package io.quartic.management;

import io.quartic.catalogue.api.*;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Path("/")
public class ManagementResource {
    private final GcsConnector gcsConnector;
    private final CatalogueService catalogueService;

    public ManagementResource(CatalogueService catalogueService, GcsConnector gcsConnector) {
        this.catalogueService = catalogueService;
        this.gcsConnector = gcsConnector;
    }

    @PUT
    @Consumes("application/json")
    @Path("/dataset")
    public String createDataset(CreateDatasetRequest createDatasetRequest) {
        DatasetConfig datasetConfig = createDatasetRequest.accept(new CreateDatasetRequest.Visitor<DatasetConfig>() {
                    @Override
                    public DatasetConfig visit(AbstractCreateStaticDatasetRequest request) {
                        return DatasetConfig.of(
                                request.metadata(),
                                CloudGeojsonDatasetLocator.of("/file/" + request.fileName())
                        );
                    }

                    @Override
                    public DatasetConfig visit(AbstractCreateLiveDatasetRequest request) {
                        return DatasetConfig.of(
                                request.metadata(),
                                TerminatorDatasetLocator.of("/api/" + UUID.randomUUID())
                        );
                    }
        });
        DatasetId datasetId = catalogueService.registerDataset(datasetConfig);
        return datasetId.uid();
    }

    @PUT
    @Path("/file")
    public String uploadFile(@Context HttpServletRequest request) throws IOException {
        String fileName = UUID.randomUUID().toString();
        gcsConnector.put(request.getContentType(), fileName, request.getInputStream());
        return fileName;
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
