package com.example.beepizcontrol.extensions.coroutines

import kotlinx.coroutines.channels.SendChannel

/**
 * Calling `offer` on a closed channel throws an exception, which is often unwanted and can lead to
 * crashes, so we're catching it here and returning false instead.
 */
fun <E> SendChannel<E>.offerCatching(element: E): Boolean {
    return runCatching { offer(element) }.getOrDefault(false)
}
