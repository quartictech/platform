package io.quartic.howl.storage

import io.quartic.howl.storage.StorageCoords.Managed
import io.quartic.howl.storage.StorageCoords.Unmanaged

val StorageCoords.bucketKey get() = when(this) {
    is Managed -> ".quartic/${identityNamespace}/${objectKey}"
    is Unmanaged -> "raw/${objectKey}"
}
