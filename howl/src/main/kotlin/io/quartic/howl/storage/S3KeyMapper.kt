package io.quartic.howl.storage

fun mapForS3(coords: StorageCoords) =
    if (coords.objectKey.startsWith("raw/")) {
        coords.objectKey
    } else {
        ".quartic/${coords.identityNamespace}/${coords.objectKey}"
    }
