package ch.orellbuehler.acolyte.api

internal actual fun transferDirectory(): String = System.getProperty("java.io.tmpdir") ?: "/data/local/tmp"
