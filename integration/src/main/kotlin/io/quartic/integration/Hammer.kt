package io.quartic.integration

import io.quartic.common.client.ClientBuilder
import io.quartic.common.model.CustomerId
import io.quartic.eval.api.EvalTriggerServiceClient
import io.quartic.eval.api.model.BuildTrigger
import java.io.File
import java.net.URI
import java.time.Instant

object Hammer {
    val uri = URI.create("http://localhost:8210/api")

    @JvmStatic
    fun main(args: Array<String>) {
        val clientBuilder = ClientBuilder(Hammer::class.java)
        val eval = clientBuilder.retrofit<EvalTriggerServiceClient>(uri)

        val output = File("output.csv").printWriter()
        var count = 0

        while (true) {
            count += 1
            try {
                eval.triggerAsync(BuildTrigger.Manual(
                    "mchammer",
                    Instant.now(),
                    CustomerId(115),
                    "master",
                    BuildTrigger.TriggerType.EVALUATE
                )).get()
                output.write("${count}, 0\n")
            }
            catch (e: Exception) {
                output.write("${count}, 1\n")
            }

            output.flush()

            Thread.sleep(5000)
        }
    }
}
