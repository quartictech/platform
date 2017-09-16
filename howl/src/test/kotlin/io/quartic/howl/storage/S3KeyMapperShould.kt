package io.quartic.howl.storage

import io.quartic.howl.storage.NoobCoords.StorageCoords
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test

class S3KeyMapperShould {

    @Test
    fun map_raw_paths_to_root_raw_key_and_ignore_namespace() {
        assertThat(mapForS3(StorageCoords("target", "identity", "raw/bar/baz")),
            equalTo("raw/bar/baz"))
    }

    @Test
    fun map_non_raw_paths_to_dot_quartic_top_level_folder_with_identity_namespace() {
        assertThat(mapForS3(StorageCoords("target", "identity", "bar/baz")),
            equalTo(".quartic/identity/bar/baz"))
    }
}
