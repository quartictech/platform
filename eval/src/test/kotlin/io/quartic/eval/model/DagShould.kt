package io.quartic.eval.model

import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.quarty.model.Step
import org.junit.Test
import org.hamcrest.Matchers.*
import org.hamcrest.MatcherAssert.*

class DagShould {
    val steps =  OBJECT_MAPPER.readValue<List<Step>>(javaClass.getResourceAsStream("/steps.json"))

    @Test
    fun build_basaic_dag() {
        val dag = Dag.fromSteps(steps)
        assertThat(dag.validate(), equalTo(true))
    }
}
