package com.example.beepizcontrol.extensions.android.os

import android.os.Handler
import android.os.Message
import com.example.beepizcontrol.extensions.coroutines.offerCatching
import kotlinx.coroutines.channels.SendChannel
import java.lang.ref.WeakReference

/**
 * A Handler that maps values received in [handleMessage] using [map], and offers the non null ones
 * to the passed [SendChannel]. It holds it in a WeakReference to avoid Handler leak.
 */
class ChannelMessageHandler<T : Any>(
    channel: SendChannel<T>,
    private val map: (Message) -> T?
) : Handler() {

    private val weakChannelReference = WeakReference(channel)

    override fun handleMessage(msg: Message) {
        weakChannelReference.get()?.let { channel ->
            val mappedValue = map(msg)
            if (mappedValue != null) channel.offerCatching(mappedValue)
        }
    }
}
