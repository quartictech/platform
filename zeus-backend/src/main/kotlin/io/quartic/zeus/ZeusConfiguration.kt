package io.quartic.zeus

import io.dropwizard.Configuration

data class ZeusConfiguration(
        val message: String
) : Configuration()