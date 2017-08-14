package io.quartic.bild.store

import io.quartic.bild.model.BildId
import io.quartic.bild.model.BildJob
import io.quartic.bild.model.BildPhase
import io.quartic.bild.model.JobResult
import io.quartic.common.model.CustomerId

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
    private val results = hashMapOf<BildId, JobRecord>()
    private val latestBuild = hashMapOf<CustomerId, BildId>()

    override fun createJob(customerId: CustomerId, installationId: Long, cloneUrl: String, ref: String, commit: String, phase: BildPhase): BildId {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

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

