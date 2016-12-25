package io.quartic.management;

import io.quartic.catalogue.api.CatalogueService;
import io.quartic.catalogue.api.CloudGeoJsonDatasetLocatorImpl;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetConfigImpl;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.catalogue.api.TerminationId;
import io.quartic.catalogue.api.TerminationIdImpl;
import io.quartic.catalogue.api.TerminatorDatasetLocatorImpl;
import io.quartic.common.geojson.GeoJsonParser;
import io.quartic.common.uid.UidGenerator;
import io.quartic.howl.api.HowlService;
import io.quartic.howl.api.HowlStorageId;
import io.quartic.management.conversion.CsvConverter;
import io.quartic.management.conversion.GeoJsonConverter;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static io.quartic.common.uid.UidUtilsKt.randomGenerator;
import static java.util.Collections.emptyMap;

@Path("/")
public class ManagementResource {
    private static final String HOWL_NAMESPACE = "management";
    private final CatalogueService catalogueService;
    private final UidGenerator<TerminationId> terminatorEndpointIdGenerator = randomGenerator(TerminationIdImpl::of);
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
                try {
                    String name = preprocessFile(request.fileName(), request.fileType());
                    return DatasetConfigImpl.of(
                            request.metadata(),
                            CloudGeoJsonDatasetLocatorImpl.of(String.format("/%s/%s", HOWL_NAMESPACE, name)),
                            emptyMap()
                    );
                }
                catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException("exception while preprocessing file: " + e);
                }
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

    private String preprocessFile(String fileName, FileType fileType) throws IOException {
        InputStream inputStream = howlService.downloadFile(HOWL_NAMESPACE, fileName);

        switch (fileType) {
            case GEOJSON:
                try {
                    new GeoJsonParser(inputStream).validate();
                } catch (IOException e) {
                    throw new BadRequestException("exception while valiodating geojson: " + e);
                }
                return fileName;
            case CSV:
                GeoJsonConverter converter = new CsvConverter();
                HowlStorageId storageId = howlService.uploadFile(MediaType.APPLICATION_JSON, HOWL_NAMESPACE,
                        outputStream -> {
                            try {
                                converter.convert(inputStream, outputStream);
                            } catch (IOException e) {
                                e.printStackTrace();
                                throw new BadRequestException("exception while converting csv to geojson: " + e);
                            }
                        });
                return storageId.uid();
            default:
                return fileName;
        }
    }

    @POST
    @Path("/file")
    @Produces(MediaType.APPLICATION_JSON)
    public HowlStorageId uploadFile(@Context HttpServletRequest request) throws IOException {
        return howlService.uploadFile(request.getContentType(), HOWL_NAMESPACE,
                outputStream -> {
                    try {
                        IOUtils.copy(request.getInputStream(), outputStream);
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException("exception while uploading file: " + e);
                    }
                });
    }
}
