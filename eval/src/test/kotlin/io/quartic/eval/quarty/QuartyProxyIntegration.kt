package io.quartic.eval.quarty

import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.quarty.api.model.Pipeline
import io.quartic.quarty.api.model.QuartyRequest
import io.quartic.quarty.api.model.QuartyResponse
import kotlinx.coroutines.experimental.runBlocking
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.*
import org.junit.Ignore
import org.junit.Test
import java.net.URI

@Ignore
class QuartyProxyIntegration {
    private val quartyProxy = QuartyProxy("localhost")
    private val devNull = { _:String, _:String ->  }

    @Test
    fun run() {
        runBlocking {
            val initialise = quartyProxy.request(QuartyRequest.Initialise(
                repoURL = URI.create("test.bundle"),
                repoCommit = "HEAD"
            ), devNull)
            assertThat(initialise as? QuartyResponse.Complete.Result, notNullValue())
            val evaluate = quartyProxy.request(QuartyRequest.Evaluate(), devNull) as? QuartyResponse.Complete.Result
            assertThat(evaluate, notNullValue())

            val pipeline = OBJECT_MAPPER.convertValue(evaluate?.result, Pipeline::class.java)
            assertThat(pipeline.nodes.size, equalTo(2))

            // This is expected to error due to no access to Howl
            val execute = quartyProxy.request(QuartyRequest.Execute(pipeline.nodes[0].id, "noob"), devNull)
                as? QuartyResponse.Complete.Error
            assertThat(execute, notNullValue())
        }
    }
}
