package io.quartic.zeus

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.dropwizard.Configuration
import io.quartic.zeus.model.DatasetName

data class ZeusConfiguration(
        val datasets: Map<DatasetName, DataProviderConfiguration>
) : Configuration()

// TODO: is there a sweeter way to automate Jackson polymorphism for sealed classes?
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
        JsonSubTypes.Type(value = ClasspathDataProviderConfiguration::class, name = "classpath")
)
sealed class DataProviderConfiguration

data class ClasspathDataProviderConfiguration(val resourceName: String) : DataProviderConfiguration()