package io.quartic.mgmt;

import com.google.common.collect.ImmutableMap;
import io.quartic.catalogue.api.CatalogueService;
import io.quartic.catalogue.api.model.CloudGeoJsonDatasetLocator;
import io.quartic.catalogue.api.model.DatasetConfig;
import io.quartic.catalogue.api.model.DatasetId;
import io.quartic.catalogue.api.model.DatasetNamespace;
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
import javax.ws.rs.NotFoundException;
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
    private final CatalogueService catalogue;
    private final HowlService howl;
    private final DatasetNamespace defaultCatalogueNamespace;

    public MgmtResource(CatalogueService catalogue, HowlService howl, DatasetNamespace defaultCatalogueNamespace) {
        this.catalogue = catalogue;
        this.howl = howl;
        this.defaultCatalogueNamespace = defaultCatalogueNamespace;
    }

    @GET
    @Path("/dataset")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<DatasetId, DatasetConfig> getDatasets() {
        return ImmutableMap.copyOf(catalogue.getDatasets().getOrDefault(defaultCatalogueNamespace, emptyMap()));
    }

    @DELETE
    @Path("/dataset/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public void deleteDataset(@PathParam("id") DatasetId id) {
        catalogue.deleteDataset(defaultCatalogueNamespace, id);
    }

    @POST
    @Path("/dataset")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public DatasetId createDataset(CreateDatasetRequest createDatasetRequest) {
        DatasetConfig datasetConfig = createDatasetRequest.accept(request -> {
            try {
                String name = preprocessFile(request.fileName(), request.fileType());
                return new DatasetConfig(
                        request.metadata(),
                        new CloudGeoJsonDatasetLocator(String.format("/%s/%s", HOWL_NAMESPACE, name), false),
                        emptyMap()
                );
            }
            catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("exception while preprocessing file: " + e);
            }
        });
        return catalogue.registerDataset(defaultCatalogueNamespace, datasetConfig).getId();
    }

    private String preprocessFile(String fileName, FileType fileType) throws IOException {
        InputStream inputStream = howl.downloadFile(HOWL_NAMESPACE, fileName)
                .orElseThrow(() -> new NotFoundException("file not found: " + fileName));

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
                HowlStorageId storageId = howl.uploadFile(MediaType.APPLICATION_JSON, HOWL_NAMESPACE,
                        outputStream -> {
                            try {
                                converter.convert(inputStream, outputStream);
                            } catch (IOException e) {
                                e.printStackTrace();
                                throw new BadRequestException("Exception while converting csv to geojson: " + e);
                            }
                        });
                return storageId.getUid();
            case RAW:
            default:
                return fileName;
        }
    }

    @POST
    @Path("/file")
    @Produces(MediaType.APPLICATION_JSON)
    public HowlStorageId uploadFile(@Context HttpServletRequest request) throws IOException {
        return howl.uploadFile(request.getContentType(), HOWL_NAMESPACE,
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
