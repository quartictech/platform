package io.quartic.mgmt;

import io.quartic.catalogue.api.CatalogueService;
import io.quartic.catalogue.api.CloudGeoJsonDatasetLocatorImpl;
import io.quartic.catalogue.api.DatasetConfig;
import io.quartic.catalogue.api.DatasetConfigImpl;
import io.quartic.catalogue.api.DatasetId;
import io.quartic.common.geojson.GeoJsonParser;
import io.quartic.howl.api.HowlService;
import io.quartic.howl.api.HowlStorageId;
import io.quartic.mgmt.conversion.CsvConverter;
import io.quartic.mgmt.conversion.GeoJsonConverter;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static java.util.Collections.emptyMap;

@Path("/")
public class MgmtResource {
    private static final String HOWL_NAMESPACE = "mgmt";
    private final CatalogueService catalogueService;
    private final HowlService howlService;

    public MgmtResource(CatalogueService catalogueService, HowlService howlService) {
        this.catalogueService = catalogueService;
        this.howlService = howlService;
    }

    @GET
    @Path("/dataset")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<DatasetId, DatasetConfig> getDatasets() {
        return catalogueService.getDatasets();
    }

    @DELETE
    @Path("/dataset/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public void deleteDataset(@PathParam("id") DatasetId id) {
        catalogueService.deleteDataset(id);
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
        });
        return catalogueService.registerDataset(datasetConfig);
    }

    private String preprocessFile(String fileName, FileType fileType) throws IOException {
        InputStream inputStream = howlService.downloadFile(HOWL_NAMESPACE, fileName);

        switch (fileType) {
            case GEOJSON:
                try {
                    new GeoJsonParser(inputStream).validate();
                } catch (Exception e) {
                    throw new BadRequestException("Exception while validating geojson: " + e);
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
                                throw new BadRequestException("Exception while converting csv to geojson: " + e);
                            }
                        });
                return storageId.getUid();
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
