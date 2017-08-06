package io.quartic.bild

import org.skife.jdbi.v2.sqlobject.Bind
import org.skife.jdbi.v2.sqlobject.SqlQuery
import org.skife.jdbi.v2.sqlobject.SqlUpdate

interface BildDao {
    @SqlUpdate("""
        create table build(
            id serial,
            customer_id varchar,
            revision varchar,
            phase varchar,
            logs text,
            build_date timestamp)
    """)
    fun createBuildsTable()

    @SqlUpdate("""
        insert into build(
            customer_id,
            revision,
            phase,
            logs,
            build_date)
        values(:customer_id, :revision, :phase, :logs, :build_date)
    """)

    @SqlQuery("select * from build where customer_id=:customerId id=:id")
    fun buildById(@Bind("customerId") customerId: String,
                  @Bind("id") id: String): Build
}
