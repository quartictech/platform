package io.quartic.eval

import io.quartic.eval.api.model.TriggerDetails
import io.quartic.eval.apis.Database.BuildResult
import io.quartic.hey.api.*
import java.time.Clock
import java.time.ZoneOffset

class Notifier(
    private val client: HeyClient,
    private val clock: Clock = Clock.systemUTC()
) {
    fun notifyAbout(trigger: TriggerDetails, result: BuildResult) {
        // TODO - include build number in title
        // TODO - add a URL to results page or whatever
        // TODO - more useful description of internal vs. user errors
        client.notify(HeyNotification(listOf(
            HeyAttachment(
                title = when (result) {
                    is BuildResult.Success -> "Build succeeded"
                    else -> "Build failed"
                },
                text = when (result) {
                    is BuildResult.Success -> "Success"
                    is BuildResult.InternalError -> result.throwable.message ?: "Internal error"
                    is BuildResult.UserError -> "Failure" // TODO: Add more detail here
                },
                fields = listOf(
                    HeyField("Repo", trigger.repoName, true),
                    HeyField("Branch", trigger.ref, true)
                ),
                timestamp = clock.instant().atOffset(ZoneOffset.UTC),
                color = when (result) {
                    is BuildResult.Success -> HeyColor.GOOD
                    else -> HeyColor.DANGER
                }
            )
        )))
    }
}
