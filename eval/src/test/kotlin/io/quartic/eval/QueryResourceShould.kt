package io.quartic.eval

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.quartic.common.model.CustomerId
import io.quartic.common.test.assertThrows
import io.quartic.eval.api.model.*
import io.quartic.eval.database.Database
import io.quartic.eval.database.Database.EventRow
import io.quartic.eval.database.model.BuildEvent
import io.quartic.eval.database.model.CurrentPhaseCompleted.Result.Success
import io.quartic.eval.database.model.LegacyPhaseCompleted.V1.Dataset
import io.quartic.eval.database.model.LegacyPhaseCompleted.V2.Artifact.EvaluationOutput
import io.quartic.eval.database.model.LegacyPhaseCompleted.V2.Node.Raw
import io.quartic.eval.database.model.LegacyPhaseCompleted.V2.Node.Step
import io.quartic.eval.database.model.PhaseCompleted
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
    fun convert_dag_to_cytoscape() {
        val a = Raw("111", mock(), mock(),
            dataset("A")
        )
        val b = Raw("222", mock(), mock(),
            dataset("B")
        )
        val c = Raw("333", mock(), mock(),
            dataset("C")
        )
        val d = Step("444", mock(),
            listOf(dataset("A"), dataset("B")),
            dataset("D")
        )
        val e = Step("555", mock(),
            listOf(dataset("B"), dataset("C"), dataset("D")),
            dataset("E")
        )
        val nodes = listOf(a, b, c, d, e)

        whenever(database.getEventsForBuild(customerId, 1234)).thenReturn(listOf(
            eventRow(PhaseCompleted(UUID.randomUUID(), Success(EvaluationOutput(nodes))))
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
                    edge(0, "A", "D"),
                    edge(1, "B", "D"),
                    edge(2, "B", "E"),
                    edge(3, "C", "E"),
                    edge(4, "D", "E")
                )
            )
        ))
    }

    @Test
    fun show_nothing_for_null_namespaces() {
        val nodes = listOf(
            Raw(
                id = "123",
                info = mock(),
                output = dataset("A", null),
                source = mock()
            )
        )

        whenever(database.getEventsForBuild(customerId, 1234)).thenReturn(listOf(
            eventRow(PhaseCompleted(UUID.randomUUID(), Success(EvaluationOutput(nodes))))
        ))

        assertThat(resource.getDag(customerId, 1234), equalTo(
            CytoscapeDag(
                setOf(
                    node("A", "raw", "")        // Note null becomes empty string
                ),
                emptySet()
            )
        ))
    }

    private fun node(id: String, type: String, namespace: String = "test") =
        CytoscapeNode(CytoscapeNodeData("${namespace}::${id}", "${namespace}::${id}", type))

    private fun edge(id: Long, source: String, target: String, namespace: String = "test") =
        CytoscapeEdge(CytoscapeEdgeData(id, "${namespace}::${source}", "${namespace}::${target}"))

    private fun dataset(id: String, namespace: String? = "test") = Dataset(namespace, id)

    private fun eventRow(event: BuildEvent) =
        EventRow(UUID.randomUUID(), UUID.randomUUID(), Instant.now(), event)
}
