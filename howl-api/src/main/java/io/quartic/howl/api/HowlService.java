package io.quartic.howl.api;

import feign.Body;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
    CloudStorageId uploadFile(@HeaderParam("Content-Type") String contentType,
                              @PathParam("namespace") String namespace,
                              InputStream inputStream);

    @GET
    @Path("/{fileName}")
    Response downloadFile(@PathParam("namespace") String namespace,
                          @PathParam("fileName") String fileName);
}
