package io.quartic.bild.store

import io.quartic.bild.model.BuildId
import io.quartic.bild.model.BuildJob
import io.quartic.bild.model.BuildPhase
import io.quartic.bild.model.JobResult
import io.quartic.common.model.CustomerId
import org.flywaydb.core.Flyway
import org.jdbi.v3.core.Jdbi
import java.time.Instant
import javax.sql.DataSource

class PostgresJobResultStore(dataSource: DataSource, val dbi: Jdbi) : JobResultStore {
    private val dao = dbi.onDemand(BildDao::class.java)

    init {
        val flyway = Flyway()
        flyway.dataSource = dataSource
        flyway.migrate()
    }

    override fun createJob(customerId: CustomerId, installationId: Long, cloneUrl: String, ref: String, commit: String, phase: BuildPhase): BuildId {
        return BuildId(dao.createBuild(customerId, installationId, cloneUrl, ref, commit, phase, Instant.now()).toString())
    }

    override fun putJobResult(job: BuildJob, jobResult: JobResult) {
        dbi.useTransaction<Exception> { h ->
            val txnDao = h.attach(BildDao::class.java)
            jobResult.logOutputByPod.forEach { podName, log -> txnDao.insertJob(job.id, podName, log) }
            txnDao.setResult(job.id, jobResult.success, jobResult.reason)
        }
    }

    override fun putDag(id: BuildId, dag: Any?) {
        dao.setDag(id, dag)
    }

    override fun getLatest(id: CustomerId): JobRecord? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
