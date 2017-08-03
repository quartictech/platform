package io.quartic.bild

import io.quartic.bild.model.BildId
import io.quartic.bild.model.BildJob
import io.quartic.bild.model.CustomerId
import io.quartic.bild.model.JobResult


class JobResultStore {
    data class Record (
        var jobResult: JobResult?,
        var extraData: Any?
    )

    private val results = hashMapOf<BildId, Record>()
    private val latestBuild = hashMapOf<CustomerId, BildId>()

    fun putJobResult(job: BildJob, jobResult: JobResult) {
        synchronized(results, {
            results.computeIfAbsent(job.id, { Record(null, null) }).jobResult = jobResult
        })

        synchronized(latestBuild, {
            latestBuild.put(job.customerId, job.id)
        })
    }

    fun putExtraData(id: BildId, extraData: Any?) {
         synchronized(results, {
            results.computeIfAbsent(id, { Record(null, null) }).extraData = extraData
        })
    }

    fun getLatest(id: CustomerId) = results.get(latestBuild.get(id))
}
