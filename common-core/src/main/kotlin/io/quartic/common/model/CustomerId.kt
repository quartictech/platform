package io.quartic.common.model

import io.quartic.common.uid.Uid

// TODO - this needs to move to a more centralised model, as it's going to be used everywhere
// TODO: I would rather this was a data class but then it overrides the toString method
class CustomerId(id: String): Uid(id) {
    constructor(id: Long) : this(id.toString())
}

