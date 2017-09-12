package io.quartic.eval

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.quartic.common.model.CustomerId
import io.quartic.common.test.assertThrows
import io.quartic.eval.Database.EventRow
import io.quartic.eval.api.model.*
import io.quartic.eval.model.BuildEvent
import io.quartic.eval.model.BuildEvent.PhaseCompleted
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success
import io.quartic.eval.model.BuildEvent.PhaseCompleted.Result.Success.Artifact.EvaluationOutput
import io.quartic.quarty.api.model.Dataset
import io.quartic.quarty.api.model.Step
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
            resource.getDag(customerId)
        }
    }

    @Test
    fun query_latest_build_number_and_use_in_subsequent_query() {
        whenever(database.getLatestSuccessfulBuildNumber(customerId)).thenReturn(5678)

        try {
            resource.getDag(customerId)
        } catch(e: Exception) {}   // Swallow

        verify(database).getEventsForBuild(customerId, 5678)
    }

    @Test
    fun convert_dag_to_cytoscape() {
        val steps = listOf(
            Step(
                id = "123",
                name = "foo",
                description = "whatever",
                file = "whatever",
                lineRange = emptyList(),
                inputs = listOf(dataset("A"), dataset("B")),
                outputs = listOf(dataset("D"))
            ),
            Step(
                id = "456",
                name = "bar",
                description = "whatever",
                file = "whatever",
                lineRange = emptyList(),
                inputs = listOf(dataset("B"), dataset("C"), dataset("D")),
                outputs = listOf(dataset("E"))
            )
        )

        whenever(database.getEventsForBuild(customerId, 1234)).thenReturn(listOf(
            eventRow(PhaseCompleted(UUID.randomUUID(), Success(EvaluationOutput(steps))))
        ))

        assertThat(resource.getDag(customerId, 1234), equalTo(
            CytoscapeDag(
                setOf(
                    node("A", "raw"),
                    node("B", "raw"),
                    node("C", "raw"),
                    node("D", "derived"),
                    node("E", "derived")
                ),
                setOf(
                    edge(1, "A", "D"),
                    edge(2, "B", "D"),
                    edge(3, "B", "E"),
                    edge(4, "C", "E"),
                    edge(0, "D", "E")
                )
            )
        ))
    }

    @Test
    fun show_nothing_for_null_namespaces() {
        val steps = listOf(
            Step(
                id = "123",
                name = "foo",
                description = "whatever",
                file = "whatever",
                lineRange = emptyList(),
                inputs = listOf(dataset("A", null)),
                outputs = listOf(dataset("B", null))
            )
        )

        whenever(database.getEventsForBuild(customerId, 1234)).thenReturn(listOf(
            eventRow(PhaseCompleted(UUID.randomUUID(), Success(EvaluationOutput(steps))))
        ))

        assertThat(resource.getDag(customerId, 1234), equalTo(
            CytoscapeDag(
                setOf(
                    node("A", "raw", ""),       // Note null becomes empty string
                    node("B", "derived", "")
                ),
                setOf(
                    edge(0, "A", "B", "")
                )
            )
        ))
    }

    private fun node(id: String, type: String, namespace: String = "test") =
        CytoscapeNode(CytoscapeNodeData("${namespace}::${id}", "${namespace}::${id}", type))

    private fun edge(id: Long, source: String, target: String, namespace: String = "test") =
        CytoscapeEdge(CytoscapeEdgeData(id, "${namespace}::${source}", "${namespace}::${target}"))

    private fun dataset(id: String, namespace: String? = "test") = Dataset(namespace, id)

    private fun eventRow(event: BuildEvent) =
        EventRow(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now(), event)
}
