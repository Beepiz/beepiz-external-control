package com.example.beepizcontrol.extensions.android.content

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.example.beepizcontrol.extensions.coroutines.offerCatching
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Attempts to bind to the [service], runs [block] once connected, and cancels it on disconnection.
 * Finally unbinds the service.
 *
 * This function is cancellable. It is recommended to use a proper scope that cancels when the
 * connecting component is destroyed, or stopped.
 *
 * You may want to use this in a loop.
 */
@Throws(SecurityException::class)
suspend fun Context.withBoundService(
    service: Intent,
    flags: Int = Context.BIND_AUTO_CREATE,
    block: suspend (serviceBinder: IBinder) -> Unit
) {
    val binderChannel = Channel<IBinder?>(capacity = Channel.CONFLATED)

    val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binderChannel.offerCatching(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binderChannel.offerCatching(null)
        }
    }

    try {
        if (bindService(service, connection, flags)) coroutineScope {
            binderChannel.consume {
                val serviceBinder = receive() ?: return@coroutineScope
                val job = launch {
                    block(serviceBinder)
                }
                receive() // Second value always comes from onServiceDisconnected.
                job.cancel()
            }
        }
    } finally {
        unbindService(connection)
    }
}
