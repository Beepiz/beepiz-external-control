package com.example.beepizcontrol.extensions.coroutines

import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

suspend inline fun repeatWhileActive(block: () -> Unit): Nothing {
    while (true) {
        coroutineContext.ensureActive()
        block()
    }
}
