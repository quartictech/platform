package io.quartic.common.application

import io.dropwizard.Configuration

abstract class ConfigurationBase : Configuration() {
    lateinit var base64EncodedJwtKey: String
}
