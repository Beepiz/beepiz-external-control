package com.example.beepizcontrol.extensions.coroutines

import com.example.beepizcontrol.extensions.collections.forEachByIndex
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED as Undispatched

/**
 * Pass at least one racer to use [raceOf].
 */
@Suppress("DeprecatedCallableAddReplaceWith", "RedundantSuspendModifier")
@Deprecated("A race needs racers.", level = DeprecationLevel.ERROR) // FOOL GUARD, DO NOT REMOVE
suspend fun <T> raceOf(): T = throw UnsupportedOperationException("A race needs racers.")

/**
 * Races all the [racers] concurrently. Once the winner completes, all other racers are cancelled,
 * then the value of the winner is returned.
 */
suspend fun <T> raceOf(vararg racers: suspend kotlinx.coroutines.CoroutineScope.() -> T): T {
    require(racers.isNotEmpty()) { "A race needs racers." }
    return kotlinx.coroutines.coroutineScope {
        @Suppress("RemoveExplicitTypeArguments")
        (kotlinx.coroutines.selects.select<T> {
            @UseExperimental(ExperimentalCoroutinesApi::class)
            val racersAsyncList = racers.map {
                async(start = Undispatched, block = it)
            }
            racersAsyncList.forEachByIndex { racer: Deferred<T> ->
                racer.onAwait { resultOfWinner: T ->
                    racersAsyncList.forEachByIndex { deferred: Deferred<T> -> deferred.cancel() }
                    return@onAwait resultOfWinner
                }
            }
        })
    }
}
