package io.quartic.eval

// Because RuntimeException doesn't implement equals (needed for testing)
class EvaluatorException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EvaluatorException
        return message == other.message
    }

    override fun hashCode() = message!!.hashCode()
}
