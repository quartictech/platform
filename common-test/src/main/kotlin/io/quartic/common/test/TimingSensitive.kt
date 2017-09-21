package io.quartic.common.test

/**
 * Denotes a test case that's timing sensitive, so may fail on the CI server.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class TimingSensitive
