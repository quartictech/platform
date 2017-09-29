package io.quartic.quartzic

import io.quartic.common.application.ConfigurationBase
import io.quartic.qube.api.model.PodSpec
import java.net.URI

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME
import io.quartic.common.model.CustomerId
import io.quartic.eval.api.model.BuildTrigger

@JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(JobConfiguration.EvalJobConfiguration::class, name = "eval"),
    JsonSubTypes.Type(JobConfiguration.QubeJobConfiguration::class, name = "qube")
)
sealed class JobConfiguration {
    abstract val name: String
    abstract val cronSchedule: String


    data class EvalTrigger(
        val customerId: CustomerId,
        val branch: String,
        val triggerType: BuildTrigger.TriggerType
    )

    data class EvalJobConfiguration(
        override val name: String,
        override val cronSchedule: String,
        val trigger: EvalTrigger
    ): JobConfiguration()

    data class QubeJobConfiguration(
        override val name: String,
        override val cronSchedule: String,
        val pod: PodSpec
    ): JobConfiguration()
}

data class QuartzicConfiguration(
    val qubeUrl: URI,
    val evalUrl: URI,
    val jobs: List<JobConfiguration>
): ConfigurationBase()

