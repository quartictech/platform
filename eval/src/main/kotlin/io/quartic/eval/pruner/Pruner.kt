package io.quartic.eval.pruner

import io.quartic.eval.Dag
import io.quartic.eval.database.model.LegacyPhaseCompleted.V2.Node

class Pruner {
    fun acceptorFor(dag: Dag): (Node) -> Boolean {
        return { true } // TODO
    }
}
