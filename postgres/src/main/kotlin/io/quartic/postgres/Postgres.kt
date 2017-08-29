package io.quartic.postgres

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import java.io.File

class Postgres {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            EmbeddedPostgres.builder()
                .setCleanDataDirectory(false)
                .setDataDirectory(File("./data"))
                .setPort(15432)
                .start()


            while (true) {
                Thread.sleep(Long.MAX_VALUE)
            }
        }
    }
}
