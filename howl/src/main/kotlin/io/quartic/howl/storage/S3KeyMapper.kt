package io.quartic.howl.storage

fun mapForS3(coords: StorageCoords) =
    if (coords.objectName.startsWith("raw/")) {
        coords.objectName
    } else {
        ".quartic/${coords.identityNamespace}/${coords.objectName}"
    }
