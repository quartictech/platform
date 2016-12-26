package io.quartic.terminator

import io.dropwizard.Configuration

class TerminatorConfiguration : Configuration() {
    var catalogueWatchUrl: String? = null
}
