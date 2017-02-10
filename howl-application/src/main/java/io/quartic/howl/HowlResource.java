package io.quartic.howl;

import io.quartic.common.uid.UidGenerator;
import io.quartic.howl.api.HowlStorageId;
import io.quartic.howl.storage.InputStreamWithContentType;
import io.quartic.howl.storage.StorageBackend;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static io.quartic.common.uid.UidUtilsKt.randomGenerator;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;

@Path("/{namespace}")
public class HowlResource {
    private final StorageBackend storageBackend;
    private final UidGenerator<HowlStorageId> howlStorageIdGenerator = randomGenerator(HowlStorageId::new);

    public HowlResource(StorageBackend storageBackend) {
        this.storageBackend = storageBackend;
    }

    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public HowlStorageId uploadFile(@PathParam("namespace") String namespace,
                                    @Context HttpServletRequest request) throws IOException {
        HowlStorageId howlStorageId = howlStorageIdGenerator.get();
        storageBackend.put(request.getContentType(), namespace, howlStorageId.getUid(), request.getInputStream());
        return howlStorageId;
    }

    @PUT
    @Path("/{fileName}")
    public void uploadFile(@PathParam("namespace") String namespace,
        @PathParam("fileName") String fileName,
        @Context HttpServletRequest request) throws IOException {

        storageBackend.put(request.getContentType(), namespace, fileName, request.getInputStream());
    }

    private Response handleDownload(String namespace, String fileName, Long version) throws IOException {
        Optional<InputStreamWithContentType> file = storageBackend.get(namespace, fileName, version);

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

    @GET
    @Path("/{fileName}")
    public Response downloadFile(@PathParam("namespace") String namespace,
                                 @PathParam("fileName") String fileName) throws IOException {
        return handleDownload(namespace, fileName, null);
    }

    @GET
    @Path("/{fileName}/{version}")
    public Response downloadFile(@PathParam("namespace") String namespace,
                                 @PathParam("fileName") String fileName,
                                 @PathParam("version") Long version) throws IOException {
        return handleDownload(namespace, fileName, version);
    }
}
