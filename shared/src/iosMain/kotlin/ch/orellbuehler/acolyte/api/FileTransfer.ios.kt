package ch.orellbuehler.acolyte.api

import platform.Foundation.NSTemporaryDirectory

internal actual fun transferDirectory(): String = NSTemporaryDirectory().trimEnd('/')
