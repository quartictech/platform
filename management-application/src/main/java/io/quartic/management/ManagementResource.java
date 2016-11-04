package io.quartic.management;

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

    public ManagementResource(GcsConnector gcsConnector) {
        this.gcsConnector = gcsConnector;
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

    @PUT
    @Path("/live")
    public Response createLiveEndpoint() {
        return null;
    }
}
