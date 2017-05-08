package io.quartic.weyl.core.attributes

data class TimeSeriesAttribute(val series: List<TimeSeriesEntry>) : ComplexAttribute {
    data class TimeSeriesEntry(val timestamp: Long, val value: Double)
}
