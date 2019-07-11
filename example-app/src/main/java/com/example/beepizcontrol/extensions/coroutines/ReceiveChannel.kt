package com.example.beepizcontrol.extensions.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@ExperimentalCoroutinesApi
suspend fun <E> ReceiveChannel<E>.consumeEachAndCancelPrevious(
    context: CoroutineContext = EmptyCoroutineContext,
    skipEquals: Boolean = false,
    action: suspend CoroutineScope.(E) -> Unit
): Unit = coroutineScope {
    var job: Job? = null
    var previousValue: E? = null // Null is safe as first value because at first, job is null.
    consumeEach { newValue ->
        job?.let { // No skipEquals on first value since there's no job yet.
            if (skipEquals && previousValue == newValue) return@consumeEach else it.cancelAndJoin()
        }
        previousValue = newValue
        job = launch(context) {
            action(newValue)
        }
    }
}
