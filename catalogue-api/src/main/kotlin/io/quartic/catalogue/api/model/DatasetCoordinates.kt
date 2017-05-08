package io.quartic.catalogue.api.model

data class DatasetCoordinates(val namespace: DatasetNamespace, val id: DatasetId) {
    constructor(namespace: String, id: String) : this(DatasetNamespace(namespace), DatasetId(id))
}