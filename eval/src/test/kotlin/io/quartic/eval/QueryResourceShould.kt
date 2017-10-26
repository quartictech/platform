package io.quartic.eval

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.quartic.common.model.CustomerId
import io.quartic.common.test.assertThrows
import io.quartic.eval.api.model.ApiDag
import io.quartic.eval.database.Database
import io.quartic.eval.database.Database.EventRow
import io.quartic.eval.database.model.BuildEvent
import io.quartic.eval.database.model.LegacyPhaseCompleted.V1.Dataset
import io.quartic.eval.database.model.PhaseCompletedV7.Node.Raw
import io.quartic.eval.database.model.PhaseCompletedV7.Node.Step
import io.quartic.eval.database.model.PhaseCompleted
import io.quartic.eval.database.model.PhaseCompletedV7.Artifact.EvaluationOutput
import io.quartic.eval.database.model.PhaseCompletedV7.Result.Success
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import java.time.Instant
import java.util.*
import javax.ws.rs.NotFoundException

class QueryResourceShould {
    private val database = mock<Database>()
    private val resource = QueryResource(database)

    private val customerId = CustomerId("999")

    @Test
    fun throw_404_if_no_rows_found() {
        assertThrows<NotFoundException> {
            resource.getDag(customerId, 1234)
        }
    }

    @Test
    fun throw_404_if_no_latest_build_found() {
        assertThrows<NotFoundException> {
            resource.getLatestDag(customerId)
        }
    }

    @Test
    fun query_latest_build_number_and_use_in_subsequent_query() {
        whenever(database.getLatestSuccessfulBuildNumber(customerId)).thenReturn(5678)

        try {
            resource.getLatestDag(customerId)
        } catch(e: Exception) {}   // Swallow

        verify(database).getEventsForBuild(customerId, 5678)
    }

    @Test
    fun get_dag_for_build() {
        val a = Raw("111", "noob", mapOf(), mock(), mock(),
            dataset("A")
        )
        val b = Raw("222", "noob2", mapOf(), mock(), mock(),
            dataset("B")
        )
        val c = Raw("333", "noob3", mapOf(), mock(), mock(),
            dataset("C")
        )
        val d = Step("444", "noob4", mapOf(), mock(),
            listOf(dataset("A"), dataset("B")),
            dataset("D")
        )
        val e = Step("555", "noob5", mapOf(), mock(),
            listOf(dataset("B"), dataset("C"), dataset("D")),
            dataset("E")
        )
        val nodes = listOf(a, b, c, d, e)

        whenever(database.getEventsForBuild(customerId, 1234)).thenReturn(listOf(
            eventRow(PhaseCompleted(UUID.randomUUID(), Success(EvaluationOutput(nodes))))
        ))

        assertThat(resource.getDag(customerId, 1234), equalTo(
            ApiDag(
                listOf(
                    ApiDag.Node("test", "A", emptyList()),
                    ApiDag.Node("test", "B", emptyList()),
                    ApiDag.Node("test", "C", emptyList()),
                    ApiDag.Node("test", "D", listOf(0, 1)),
                    ApiDag.Node("test", "E", listOf(1, 2, 3))
                )
            )
        ))
    }

    private fun dataset(id: String, namespace: String? = "test") = Dataset(namespace, id)

    private fun eventRow(event: BuildEvent) =
        EventRow(UUID.randomUUID(), UUID.randomUUID(), Instant.now(), event)
}
