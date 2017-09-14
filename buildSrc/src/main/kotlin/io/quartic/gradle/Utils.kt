package io.quartic.gradle

fun Any.getResourceAsText(name: String) = javaClass.getResource(name).readText()

//fun CopySpec.fromResource(name: String) =
//    from(resources.text.fromString(this@FrontendPlugin.javaClass.getResource(name).readText()).asFile()) {
//        it.rename { _ -> name }
//    }
