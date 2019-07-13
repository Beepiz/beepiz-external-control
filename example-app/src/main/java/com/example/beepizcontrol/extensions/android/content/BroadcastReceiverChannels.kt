package com.example.beepizcontrol.extensions.android.content

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.example.beepizcontrol.extensions.coroutines.offerCatching
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

@Suppress("NOTHING_TO_INLINE")
inline fun Context.conflatedBroadcastReceiverChannel(
    action: String,
    priority: Int = 0,
    offerInitialEmptyIntent: Boolean = false
): ReceiveChannel<Intent> = broadcastReceiverChannel(
    action = action,
    priority = priority,
    capacity = Channel.CONFLATED,
    offerInitialEmptyIntent = offerInitialEmptyIntent
)

@Suppress("NOTHING_TO_INLINE")
inline fun Context.broadcastReceiverChannel(
    action: String,
    priority: Int = 0,
    capacity: Int = Channel.UNLIMITED,
    offerInitialEmptyIntent: Boolean = false
): ReceiveChannel<Intent> = broadcastReceiverChannel(
    filter = IntentFilter(action).also { it.priority = priority },
    capacity = capacity,
    offerInitialEmptyIntent = offerInitialEmptyIntent
)

fun Context.broadcastReceiverChannel(
    filter: IntentFilter,
    capacity: Int = Channel.UNLIMITED,
    offerInitialEmptyIntent: Boolean = false
): ReceiveChannel<Intent> {
    val channel = Channel<Intent>(capacity = capacity)
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            channel.offerCatching(intent)
        }
    }
    @UseExperimental(ExperimentalCoroutinesApi::class)
    channel.invokeOnClose {
        unregisterReceiver(receiver)
    }
    registerReceiver(receiver, filter)
    if (offerInitialEmptyIntent) channel.offerCatching(Intent())
    return channel
}
