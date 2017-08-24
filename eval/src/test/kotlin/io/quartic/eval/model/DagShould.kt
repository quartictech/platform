package io.quartic.eval.model

import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.quarty.model.Step
import org.junit.Test
import org.hamcrest.Matchers.*
import org.hamcrest.MatcherAssert.*

class DagShould {
    fun readSteps(file: String) =
        OBJECT_MAPPER.readValue<List<Step>>(javaClass.getResourceAsStream(file))

    @Test
    fun build_basic_dag() {
        val dag = Dag.fromSteps(readSteps("/steps.json"))
        assertThat(dag.validate(), equalTo(true))
    }

    @Test
    fun detect_cyclic_dag() {
        val dag = Dag.fromSteps(readSteps("/steps_cyclic.json"))
        assertThat(dag.validate(), equalTo(false))
    }

    @Test
    fun detect_multiple_steps_per_output() {
        val dag = Dag.fromSteps(readSteps("/steps_duplicate_outputs.json"))
        assertThat(dag.validate(), equalTo(false))
    }
}
