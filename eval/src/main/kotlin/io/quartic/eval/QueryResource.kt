package io.quartic.eval

import io.quartic.common.model.CustomerId
import io.quartic.eval.api.EvalQueryService
import io.quartic.eval.api.model.*
import io.quartic.eval.database.Database
import io.quartic.eval.model.Dag
import io.quartic.quarty.model.Dataset
import io.quartic.quarty.model.Step
import javax.ws.rs.NotFoundException

class QueryResource(private val database: Database) : EvalQueryService {
    override fun getDag(customerId: CustomerId) = convertToCytoscape(
        database.getLatestDag(customerId) ?: throw NotFoundException("No DAG registered for ${customerId}")
    )

    // TODO - We're assuming that the DAG was validated before being added to the database
    private fun convertToCytoscape(raw: List<Step>) = with(Dag.fromSteps(raw)) {
        CytoscapeDag(nodesFrom(this), edgesFrom(this))
    }

    private fun nodesFrom(dag: Dag) = dag.nodes.map {
        CytoscapeNode(
                CytoscapeNodeData(
                    id = it.title,
                    title = it.title,
                    type = if (dag.inDegreeOf(it) > 0) "derived" else "raw"
                )
        )
    }.toSet()

    private fun edgesFrom(dag: Dag) = dag.edges.mapIndexed { i, it ->
        CytoscapeEdge(
            CytoscapeEdgeData(
                id = i.toLong(),
                source = it.source.title,
                target = it.target.title
            )
        )
    }.toSet()

    private val Dataset.title get() = "${namespace}::${datasetId}"
}
