package io.quartic.management;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;
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
        InputStreamWithContentType file = gcsConnector.get(fileName);
        return Response.ok()
                .header("Content-Type", file.contentType())
                .entity(file.inputStream())
                .build();
    }

    @PUT
    @Path("/live")
    public Response createLiveEndpoint() {
        return null;
    }
}
