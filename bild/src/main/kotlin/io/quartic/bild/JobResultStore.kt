package io.quartic.bild

import io.quartic.bild.model.BildId
import io.quartic.bild.model.BildJob
import io.quartic.bild.model.JobResult
import io.quartic.common.model.CustomerId

class JobResultStore {
    data class Record (
        val jobResult: JobResult?,
        val dag: Any?
    )

    private val results = hashMapOf<BildId, Record>()
    private val latestBuild = hashMapOf<CustomerId, BildId>()

    fun putJobResult(job: BildJob, jobResult: JobResult) {
        synchronized(results, {
            results[job.id] = (results[job.id] ?: Record(null, null)).copy(jobResult = jobResult)
        })

        synchronized(latestBuild, {
            latestBuild.put(job.customerId, job.id)
        })
    }

    fun putDag(id: BildId, dag: Any?) {
         synchronized(results, {
             results[id] = (results[id] ?: Record(null, null)).copy(dag = dag)
        })
    }

    fun getLatest(id: CustomerId): JobResultStore.Record? {
        val latestBuild = latestBuild.get(id)

        if (latestBuild != null) {
            return results[latestBuild]
        }
        return null
    }
}
