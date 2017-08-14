package io.quartic.bild.store

import io.quartic.bild.model.BuildId
import io.quartic.bild.model.BuildJob
import io.quartic.bild.model.BuildPhase
import io.quartic.bild.model.JobResult
import io.quartic.common.model.CustomerId

data class JobRecord (
    val jobResult: JobResult?,
    val dag: Any?
)

interface JobResultStore {
    fun createJob(customerId: CustomerId, installationId: Long, cloneUrl: String,
                  ref: String, commit: String, phase: BuildPhase): BuildId
    fun putJobResult(job: BuildJob, jobResult: JobResult)
    fun putDag(id: BuildId, dag: Any?)
    fun getLatest(id: CustomerId): JobRecord?
}

class EmbeddedJobResultStore : JobResultStore {
    private val results = hashMapOf<BuildId, JobRecord>()
    private val latestBuild = hashMapOf<CustomerId, BuildId>()

    override fun createJob(customerId: CustomerId, installationId: Long, cloneUrl: String, ref: String, commit: String, phase: BuildPhase): BuildId {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun putJobResult(job: BuildJob, jobResult: JobResult) {
        synchronized(results, {
            results[job.id] = (results[job.id] ?: JobRecord(null, null)).copy(jobResult = jobResult)
        })

        synchronized(latestBuild, {
            latestBuild.put(job.customerId, job.id)
        })
    }

    override fun putDag(id: BuildId, dag: Any?) {
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

