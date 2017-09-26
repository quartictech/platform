package io.quartic.howl.storage

import io.quartic.howl.storage.StorageCoords.Managed
import io.quartic.howl.storage.StorageCoords.Unmanaged
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test

class StorageCoordsShould {

    @Test
    fun map_unmanaged_to_root_raw_key() {
        assertThat(Unmanaged("target", "bar/baz").backendKey, equalTo("raw/bar/baz"))
    }

    @Test
    fun map_managed_to_dot_quartic_top_level_folder_with_identity_namespace() {
        assertThat(Managed("target", "identity", "bar/baz").backendKey, equalTo(".quartic/identity/bar/baz"))
    }
}
