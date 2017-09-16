package io.quartic.howl.storage

import io.quartic.howl.storage.NoobCoords.StorageCoords

fun mapForS3(coords: StorageCoords) =
    if (coords.objectKey.startsWith("raw/")) {
        coords.objectKey
    } else {
        ".quartic/${coords.identityNamespace}/${coords.objectKey}"
    }
