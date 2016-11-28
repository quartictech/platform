package io.quartic.management.storage;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageScopes;
import com.google.api.services.storage.model.StorageObject;
import io.quartic.management.InputStreamWithContentType;
import io.quartic.management.InputStreamWithContentTypeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Optional;

public class GcsStorageBackend implements StorageBackend {
    private static final Logger LOG = LoggerFactory.getLogger(GcsStorageBackend.class);
    private final Storage storage;
    private final String bucketName;

    private static Storage buildService() throws IOException, GeneralSecurityException {
        HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory jsonFactory = new JacksonFactory();
        GoogleCredential credential = GoogleCredential.getApplicationDefault(transport, jsonFactory);

        // Depending on the environment that provides the default credentials (for
        // example: Compute Engine, App Engine), the credentials may require us to
        // specify the scopes we need explicitly.  Check for this case, and inject
        // the Cloud Storage scope if required.
        if (credential.createScopedRequired()) {
            Collection<String> scopes = StorageScopes.all();
            credential = credential.createScoped(scopes);
        }

        return new Storage.Builder(transport, jsonFactory, credential)
                .setApplicationName("Jester Management Service")
                .build();
    }

    public GcsStorageBackend(String bucketName) throws IOException, GeneralSecurityException {
        this.storage = buildService();
        this.bucketName = bucketName;
    }

    @Override
    public Optional<InputStreamWithContentType> get(String objectName) throws IOException {
        Storage.Objects.Get get = storage.objects().get("quartic-test", objectName);
        try {
            HttpResponse httpResponse = get.executeMedia();
            return Optional.of(InputStreamWithContentTypeImpl.of(httpResponse.getContentType(), httpResponse.getContent()));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @Override
    public void put(String contentType, String objectName, InputStream inputStream) throws IOException {
        InputStreamContent inputStreamContent = new InputStreamContent(contentType, inputStream);
        StorageObject objectMetadata = new StorageObject()
                .setName(objectName);
        Storage.Objects.Insert insert = storage.objects().insert(bucketName, objectMetadata, inputStreamContent);
        insert.execute();
    }
}
