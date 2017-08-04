package io.quartic.bild

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.quartic.bild.model.BildId
import io.quartic.bild.model.BildJob
import io.quartic.bild.model.BildPhase
import io.quartic.bild.resource.ExecResource
import io.quartic.common.model.CustomerId
import io.quartic.common.uid.UidGenerator
import org.junit.Test
import java.util.concurrent.BlockingQueue


class ExecResourceShould {
    private val dag = mapOf("noob" to "yes")
    private val jobResults = mock<JobResultStore>()
    private val queue = mock<BlockingQueue<BildJob>>()
    private val idGenerator = mock<UidGenerator<BildId>>()
    private val resource = ExecResource(queue, jobResults, idGenerator)
    private val bildId = BildId("noob")

    init {
        whenever(jobResults.getLatest(CustomerId("111")))
            .thenReturn(JobResultStore.Record(null, dag))

        whenever(idGenerator.get()).thenReturn(bildId)
    }

    @Test
    fun exec_adds_to_queue() {
        val customerId = CustomerId("ladispute")
        resource.exec(customerId, BildPhase.TEST)
        verify(queue).put(BildJob(bildId, customerId, BildPhase.TEST))
    }
}
