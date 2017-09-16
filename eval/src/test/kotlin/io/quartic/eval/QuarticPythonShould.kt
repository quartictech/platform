package io.quartic.eval

import com.fasterxml.jackson.module.kotlin.readValue
import io.quartic.common.serdes.OBJECT_MAPPER
import io.quartic.quarty.api.model.Pipeline
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Paths

class QuarticPythonShould {
    companion object {
        const val QUARTIC_PYTHON_VERSION = "0.2.0"
        const val SCRIPT = """
        set -e
        python3 -m venv .env
        source .env/bin/activate
        pip install git+git://github.com/quartictech/quartic-python.git@${QUARTIC_PYTHON_VERSION}
        pip install requests datadiff
        """

        @ClassRule
        @JvmField
        val folder = TemporaryFolder()

        @BeforeClass
        @JvmStatic
        fun setUp() {
            folder.newFile("script.sh").writeText(SCRIPT)
            runProcess("/bin/bash", "./script.sh")
        }

        fun runProcess(vararg cmds: String) {
            val rc = ProcessBuilder(cmds.toList())
                .directory(folder.root)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .start()
                .waitFor()
            assertThat(rc, equalTo(0))
        }

        fun runScript(script: String) {
            val file = folder.newFile()
            file.writeText(script)
            runProcess("/bin/bash", file.absolutePath)
        }
    }


    @Test
    fun produce_compatible_output() {
        val pipelinesDir = folder.newFolder("pipelines")
        Paths.get(pipelinesDir.absolutePath, "__init__.py")
            .toFile().writeText("")
        Paths.get(pipelinesDir.absolutePath, "test.py")
            .toFile()
            .writeText("""
                from quartic import step

                @step
                def noob(x: "raw") -> "derived":
                    return None
                """.trimIndent())

        val outputFile = folder.newFile()

        runScript("""
                source .env/bin/activate
                python -m quartic.pipeline.runner --evaluate ${outputFile.absolutePath} \
                    --exception ${folder.newFile().absolutePath} \
                    ${pipelinesDir.absolutePath}
                """)


        OBJECT_MAPPER.readValue<Pipeline>(outputFile)
    }

}
