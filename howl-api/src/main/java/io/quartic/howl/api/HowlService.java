package io.quartic.howl.api;

import feign.Response;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
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
    Response downloadFileAsResponse(@PathParam("namespace") String namespace,
                          @PathParam("fileName") String fileName);

    default InputStream downloadFile(String namespace, String fileName) throws IOException {
       return downloadFileAsResponse(namespace, fileName).body().asInputStream();
    }
}
