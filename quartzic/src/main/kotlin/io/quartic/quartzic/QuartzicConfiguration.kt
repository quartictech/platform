package io.quartic.quartzic

import io.quartic.common.application.ConfigurationBase
import io.quartic.qube.api.model.PodSpec
import java.net.URI

data class JobConfiguration(
    val name: String,
    val cronSchedule: String,
    val pod: PodSpec
)

data class QuartzicConfiguration(
    val qubeUrl: URI,
    val evalUrl: URI,
    val jobs: List<JobConfiguration>
): ConfigurationBase()

