package com.example.beepizcontrol

import android.content.Context
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.DeadObjectException
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import androidx.annotation.RequiresPermission
import com.example.beepizcontrol.BeepizMonitoringState.MONITORING
import com.example.beepizcontrol.BeepizMonitoringState.NOT_MONITORING
import com.example.beepizcontrol.extensions.android.content.withBoundService
import com.example.beepizcontrol.extensions.android.os.ChannelMessageHandler
import com.example.beepizcontrol.extensions.coroutines.consumeEachAndCancelPrevious
import kotlinx.coroutines.channels.Channel

enum class BeepizMonitoringState {
    MONITORING, NOT_MONITORING
}

fun Context.startBeepizMonitoring() {
    sendBroadcast(Intent().also {
        it.`package` = BeepizBindingConstants.packageName
        it.action = BeepizBindingConstants.startAction
    })
}

fun Context.stopBeepizMonitoring() {
    sendBroadcast(Intent().also {
        it.`package` = BeepizBindingConstants.packageName
        it.action = BeepizBindingConstants.stopAction
    })
}

/**
 * In case the `requiresConfig` [Boolean] parameter of the [handleState] lambda is `false`,
 * you need to open the Beepiz app to let the user resolve the configuration issue.
 *
 * Each time the state changes, any ongoing execution of the [handleState] lambda will be cancelled,
 * and it will then be called again with the new state.
 */
@RequiresPermission(BeepizBindingConstants.permission)
suspend fun Context.runBeepizBindingUntilDisconnection(
    handleState: suspend (state: BeepizMonitoringState, requiresConfig: Boolean) -> Unit
) {
    val intent = Intent().also {
        it.action = BeepizBindingConstants.bindAction
        it.`package` = BeepizBindingConstants.packageName
    }
    try {
        withBoundService(intent) { serviceBinder ->
            val service = Messenger(serviceBinder)
            val incomingChannel = Channel<Pair<BeepizMonitoringState, Boolean>>(
                capacity = Channel.RENDEZVOUS
            )
            val serviceToClientMessenger = Messenger(ChannelMessageHandler(incomingChannel) { msg ->
                Pair(
                    when (msg.what) {
                        BeepizBindingConstants.CURRENTLY_MONITORING -> MONITORING
                        BeepizBindingConstants.CURRENTLY_NOT_MONITORING -> NOT_MONITORING
                        else -> return@ChannelMessageHandler null
                    }, when (msg.arg1) {
                        BeepizBindingConstants.ARG1_REQUIRES_CONFIG -> true
                        BeepizBindingConstants.ARG1_CONFIG_OK -> false
                        else -> return@ChannelMessageHandler null
                    }
                )
            })
            try {
                val registerMessage = Message.obtain().also {
                    it.what = BeepizBindingConstants.REGISTER_CLIENT
                    it.replyTo = serviceToClientMessenger
                    if (SDK_INT >= 22) it.isAsynchronous = true
                }
                service.send(registerMessage)
                @Suppress("EXPERIMENTAL_API_USAGE")
                incomingChannel.consumeEachAndCancelPrevious { (state, requiresConfig) ->
                    handleState(state, requiresConfig)
                }
            } finally {
                val registerMessage = Message.obtain().also {
                    it.what = BeepizBindingConstants.UNREGISTER_CLIENT
                    it.replyTo = serviceToClientMessenger
                    if (SDK_INT >= 22) it.isAsynchronous = true
                }
                service.send(registerMessage)
            }
        }
    } catch (ignored: RemoteException) { // Remote process died. We can only retry.
    } catch (ignored: DeadObjectException) { // Remote process died. We can only retry.
    }
}