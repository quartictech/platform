package io.quartic.howl;

import io.quartic.common.uid.RandomUidGenerator;
import io.quartic.common.uid.UidGenerator;
import io.quartic.howl.api.CloudStorageId;
import io.quartic.howl.api.CloudStorageIdImpl;
import io.quartic.howl.storage.InputStreamWithContentType;
import io.quartic.howl.storage.StorageBackend;
import org.apache.commons.io.IOUtils;

import javax.print.attribute.standard.Media;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;

@Path("/{namespace}")
public class HowlResource {
    private final StorageBackend storageBackend;
    private final UidGenerator<CloudStorageId> cloudStorageIdGenerator = RandomUidGenerator.of(CloudStorageIdImpl::of);

    public HowlResource(StorageBackend storageBackend) {
        this.storageBackend = storageBackend;
    }

    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public CloudStorageId uploadFile(@PathParam("namespace") String namespace,
                                     @Context HttpServletRequest request) throws IOException {
        CloudStorageId cloudStorageId = cloudStorageIdGenerator.get();
        System.out.println(IOUtils.toString(request.getInputStream()));
        storageBackend.put(request.getContentType(), namespace, cloudStorageId.uid(), request.getInputStream());
        return cloudStorageId;
    }

    @PUT
    @Path("/{fileName}")
    public void uploadFile(@PathParam("namespace") String namespace,
        @PathParam("fileName") String fileName,
        @Context HttpServletRequest request) throws IOException {

        storageBackend.put(request.getContentType(), namespace, fileName, request.getInputStream());
    }

    @GET
    @Path("/{fileName}")
    public Response downloadFile(@PathParam("namespace") String namespace,
                                 @PathParam("fileName") String fileName) throws IOException {
        Optional<InputStreamWithContentType> file = storageBackend.get(namespace, fileName);

        return file.map( f ->
            Response.ok()
                .header(CONTENT_TYPE, f.contentType())
                .entity(((StreamingOutput) output -> {
                    try (InputStream inputStream = f.inputStream()) {
                        IOUtils.copy(inputStream, output);
                    }
                }))
                .build())
                .orElseThrow(NotFoundException::new);
    }
}
