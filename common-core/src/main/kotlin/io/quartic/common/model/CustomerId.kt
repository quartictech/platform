package io.quartic.common.model

import io.quartic.common.uid.Uid

// TODO - this needs to move to a more centralised model, as it's going to be used everywhere
data class CustomerId(val id: String): Uid(id) {
    constructor(id: Long) : this(id.toString())
}
