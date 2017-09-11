package io.quartic.qube

import io.quartic.common.db.BindJson
import io.quartic.qube.api.QubeRequest
import io.quartic.qube.api.model.ContainerState
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import java.time.Instant
import java.util.*

/**
 * Job here represents a run of a Kubernetes Pod and *not* an actual Kubernetes Job.
 */
interface Database {
    @SqlUpdate("""insert into job(
        id,
        client,
        name,
        create_spec,
        start_time,
        end_time,
        containers
    ) values(
        :id,
        :client,
        :name,
        :create_spec,
        :start_time,
        :end_time,
        :containers)""")
    fun insertJob(
        @Bind("id") id: UUID,
        @Bind("client") client: UUID,
        @Bind("name") podName: String,
        @BindJson("create_spec") createPod: QubeRequest.Create,
        @Bind("start_time") startTime: Instant,
        @Bind("end_time") endTime: Instant,
        @BindJson("containers") containers: Map<String, ContainerState>
    )
}
