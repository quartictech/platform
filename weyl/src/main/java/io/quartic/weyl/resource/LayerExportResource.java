package io.quartic.weyl.resource;

import io.quartic.weyl.core.export.LayerExportRequest;
import io.quartic.weyl.core.export.LayerExportResult;
import io.quartic.weyl.core.export.LayerExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/export")
public class LayerExportResource {
    private static final Logger LOG = LoggerFactory.getLogger(LayerExportResource.class);
    private final LayerExporter layerExporter;

    public LayerExportResource(LayerExporter layerExporter) {
       this.layerExporter = layerExporter;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public LayerExportResult export(LayerExportRequest layerExportRequest) {
        LOG.info("layer export request received for id: {}", layerExportRequest.getLayerId());
        return layerExporter.export(layerExportRequest)
                .toBlocking()
                .first()
                .orElseThrow(() -> new NotFoundException("unable to find layer with id " + layerExportRequest.getLayerId()));
    }
}
