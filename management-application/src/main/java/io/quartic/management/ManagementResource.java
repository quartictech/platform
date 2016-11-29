package io.quartic.management;

import feign.Response;
import io.quartic.catalogue.api.*;
import io.quartic.common.serdes.ObjectMappers;
import io.quartic.common.uid.RandomUidGenerator;
import io.quartic.common.uid.UidGenerator;
import io.quartic.geojson.FeatureCollection;
import io.quartic.howl.api.HowlStorageId;
import io.quartic.howl.api.HowlService;
import io.quartic.management.conversion.CsvConverter;
import io.quartic.management.conversion.GeoJsonConverter;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
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
                String name = preprocessFile(request.fileName(), request.fileType());
                return DatasetConfigImpl.of(
                        request.metadata(),
                        CloudGeoJsonDatasetLocatorImpl.of(String.format("/%s/%s", HOWL_NAMESPACE, name)),
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

    private String preprocessFile(String fileName, FileType fileType) {
        switch (fileType) {
            case GEOJSON:
                return fileName;
            case CSV:
                GeoJsonConverter converter = new CsvConverter();
                Response response = howlService.downloadFile(HOWL_NAMESPACE, fileName);
                try {
                    // convert to GeoJSON
                    FeatureCollection featureCollection = converter.convert(response.body().asInputStream());
                    byte[] data = ObjectMappers.OBJECT_MAPPER.writeValueAsBytes(featureCollection);
                    HowlStorageId storageId = howlService.uploadFile(MediaType.APPLICATION_JSON, HOWL_NAMESPACE,
                            new ByteArrayInputStream(data));
                    return storageId.uid();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException("error while encoding feature collection: " + e);
                }
            default:
                return fileName;
        }
    }

    @POST
    @Path("/file")
    @Produces(MediaType.APPLICATION_JSON)
    public HowlStorageId uploadFile(@Context HttpServletRequest request) throws IOException {
        return howlService.uploadFile(request.getContentType(), HOWL_NAMESPACE, request.getInputStream());
    }
}
