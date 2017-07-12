package io.quartic.common.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.full.companionObject

// See http://stackoverflow.com/a/34462577/129570
fun <R : Any> R.logger(): Lazy<Logger> {
    return lazy { LoggerFactory.getLogger(unwrapCompanionClass(this.javaClass).name) }
}

fun <T: Any> unwrapCompanionClass(ofClass: Class<T>): Class<*> {
    return if (ofClass.enclosingClass != null && ofClass.enclosingClass.kotlin.companionObject?.java == ofClass) {
        ofClass.enclosingClass
    } else {
        ofClass
    }
}

