package io.quartic.howl;

import io.quartic.howl.storage.DiskStorageBackend;
import io.quartic.howl.storage.InputStreamWithContentType;
import io.quartic.howl.storage.StorageBackend;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class DiskStorageBackendShould {
    private Path tempDir;
    private StorageBackend backend;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("howl-test");
        backend = new DiskStorageBackend(tempDir);
    }

    @Test
    public void store_data_and_return_it() throws IOException {
        byte[] data = "data".getBytes();
        storeData("wat", data);
        byte[] readData = getData("wat", null);
        assertThat(readData, equalTo(data));
    }

    @Test
    public void overwrite_with_new_version() throws IOException {
        byte[] data = "data".getBytes();
        storeData("wat", data);
        byte[] data2 = "data2".getBytes();
        storeData("wat", data2);
        assertThat(getData("wat", null), equalTo(data2));
    }

    @Test
    public void writes_to_separate_objects_are_separate() throws IOException {
        storeData("wat", "data".getBytes());
        storeData("wat2", "data2".getBytes());
        assertThat(getData("wat", null), equalTo("data".getBytes()));
        assertThat(getData("wat2", null), equalTo("data2".getBytes()));
    }

    @Test
    public void overwrites_create_new_versions() throws IOException {
        Long version = storeData("leet", "data".getBytes());
        assertThat(getData("leet", version), equalTo("data".getBytes()));
        Long version2 = storeData("leet", "data2".getBytes());
        assertThat(getData("leet", version2), equalTo("data2".getBytes()));

        assertThat(getDataOptional("leet", 0L), equalTo(Optional.empty()));
    }

    private Long storeData(String objectName, byte[] data) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        return backend.put(MediaType.TEXT_PLAIN, "test", objectName, inputStream);
    }

    private Optional<byte[]> getDataOptional(String objectName, Long version) throws IOException {
        Optional<InputStreamWithContentType> inputStreamWithContentType = backend.get("test", objectName, version);
        if (inputStreamWithContentType.isPresent()) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            IOUtils.copy(inputStreamWithContentType.get().inputStream(), outputStream);
            return Optional.of(outputStream.toByteArray());
        }
        return Optional.empty();
    }

    private byte[] getData(String objectName, Long version) throws IOException {
        return getDataOptional(objectName, version).get();
    }
}
