package db.migration

import io.quartic.eval.database.PayloadMigration
import io.quartic.eval.database.model.LegacyPhaseCompleted.V4
import io.quartic.eval.database.model.LegacyPhaseCompleted.V5

@Suppress("unused")
class V5__Structure_user_errors : PayloadMigration<V4, V5>(V4::class.java, V5::class.java) {
    override fun sql() =
        """
            SELECT *
                FROM event
                WHERE
                    payload->>'type' = 'phase_completed' AND
                    payload#>>'{result,type}' = 'user_error'
        """

    override fun transform(payload: V4) = V5(
        payload.phaseId,
        V5.Result.UserError(
            V5.UserErrorInfo.OtherException(
                when (payload.result) {
                    is V4.Result.UserError -> V5.UserErrorInfo.OtherException(payload.result.detail)
                    else -> throw IllegalStateException("Can only transform UserError. Found ${payload.result}")
                }
            )
        )
    )
}
