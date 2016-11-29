package io.quartic.management;

import io.quartic.catalogue.api.*;
import io.quartic.common.uid.RandomUidGenerator;
import io.quartic.common.uid.UidGenerator;
import io.quartic.howl.api.CloudStorageId;
import io.quartic.howl.api.HowlService;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import static java.util.Collections.emptyMap;

@Path("/")
public class ManagementResource {
    private static final String HOWL_NAMESPACE = "management";
    private final CatalogueService catalogueService;
    private final UidGenerator<TerminationId> terminatorEndpointIdGenerator = RandomUidGenerator.of(TerminationIdImpl::of);
    private final HowlService howlService;

    public ManagementResource(CatalogueService catalogueService, HowlService howlService) {
        this.catalogueService = catalogueService;
        this.howlService = howlService;
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
                        CloudGeoJsonDatasetLocatorImpl.of(String.format("/%s/%s", HOWL_NAMESPACE, request.fileName())),
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
        return howlService.uploadFile(request.getContentType(), HOWL_NAMESPACE, request.getInputStream());
    }
}
