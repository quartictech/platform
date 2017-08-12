package io.quartic.bild.store

import io.quartic.bild.model.BildId
import io.quartic.bild.model.BildJob
import io.quartic.bild.model.BildPhase
import io.quartic.bild.model.JobResult
import io.quartic.common.model.CustomerId
import org.flywaydb.core.Flyway
import org.jdbi.v3.core.Jdbi
import java.time.Instant
import javax.sql.DataSource

data class JobRecord (
    val jobResult: JobResult?,
    val dag: Any?
)

interface JobResultStore {
    fun createJob(customerId: CustomerId, installationId: Long, cloneUrl: String, ref: String, commit: String, phase: BildPhase): BildId

    fun putJobResult(job: BildJob, jobResult: JobResult)
    fun putDag(id: BildId, dag: Any?)
    fun getLatest(id: CustomerId): JobRecord?
}

class EmbeddedJobResultStore : JobResultStore {
    override fun createJob(customerId: CustomerId, installationId: Long, cloneUrl: String, ref: String, commit: String, phase: BildPhase): BildId {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private val results = hashMapOf<BildId, JobRecord>()
    private val latestBuild = hashMapOf<CustomerId, BildId>()

    override fun putJobResult(job: BildJob, jobResult: JobResult) {
        synchronized(results, {
            results[job.id] = (results[job.id] ?: JobRecord(null, null)).copy(jobResult = jobResult)
        })

        synchronized(latestBuild, {
            latestBuild.put(job.customerId, job.id)
        })
    }

    override fun putDag(id: BildId, dag: Any?) {
         synchronized(results, {
             results[id] = (results[id] ?: JobRecord(null, null)).copy(dag = dag)
        })
    }

    override fun getLatest(id: CustomerId): JobRecord? {
        val latestBuild = latestBuild.get(id)

        if (latestBuild != null) {
            return results[latestBuild]
        }
        return null
    }
}

class PostgresJobResultStore(dataSource: DataSource, dbi: Jdbi) : JobResultStore {
    override fun createJob(customerId: CustomerId, installationId: Long, cloneUrl: String, ref: String, commit: String, phase: BildPhase): BildId {
        return BildId(dao.createBuild(customerId, installationId, cloneUrl, ref, commit, phase, Instant.now()).toString())
    }

    val dao = dbi.onDemand(BildDao::class.java)

    init {
        val flyway = Flyway()
        flyway.dataSource = dataSource
        flyway.migrate()
    }

    override fun putJobResult(job: BildJob, jobResult: JobResult) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun putDag(id: BildId, dag: Any?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLatest(id: CustomerId): JobRecord? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
