package io.quartic.howl.api;

import feign.Response;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;

@Path("/{namespace}")
public interface HowlService {
    @PUT
    @Path("/{fileName}")
    void uploadFile(@HeaderParam("Content-Type") String contentType,
                    @PathParam("namespace") String namespace,
                    @PathParam("fileName") String fileName,
                    InputStream inputStream);

    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    HowlStorageId uploadFile(@HeaderParam("Content-Type") String contentType,
                             @PathParam("namespace") String namespace,
                             InputStream inputStream);

    @GET
    @Path("/{fileName}")
    Response downloadFile(@PathParam("namespace") String namespace,
                          @PathParam("fileName") String fileName);
}
