package com.example.beepizcontrol

import android.app.Activity
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

/**
 * Requests start of Beepiz monitoring.
 *
 * **WARNING:** Should be called only if Beepiz doesn't requireConfig. Otherwise, the Beepiz app
 * should be launched to let the user resolve configuration issues.
 *
 * Future versions of the Beepiz app will completely ignore start requests if improperly configured.
 */
@RequiresPermission(BeepizBindingConstants.permission)
fun Context.startBeepizMonitoring() {
    sendBroadcast(Intent(BeepizBindingConstants.startAction).also {
        it.`package` = BeepizBindingConstants.packageName
    })
}

/**
 * Requests stop of Beepiz monitoring.
 */
@RequiresPermission(BeepizBindingConstants.permission)
fun Context.stopBeepizMonitoring() {
    sendBroadcast(Intent(BeepizBindingConstants.stopAction).also {
        it.`package` = BeepizBindingConstants.packageName
    })
}

/**
 * In case the `requiresConfig` [Boolean] parameter of the [handleState] lambda is `false`,
 * you need to open the Beepiz app to let the user resolve the configuration issue.
 *
 * The easiest way to do so from an [Activity] is with the following snippet:
 * `startActivity(packageManager.getLaunchIntentForPackage(BeepizBindingConstants.packageName))`
 *
 * Each time the state changes, any ongoing execution of the [handleState] lambda will be cancelled,
 * and it will then be called again with the new state.
 */
@RequiresPermission(BeepizBindingConstants.permission)
suspend fun Context.runBeepizBindingUntilDisconnection(
    handleState: suspend (state: BeepizMonitoringState, requiresConfig: Boolean) -> Unit
) {
    val intent = Intent(BeepizBindingConstants.bindAction).also {
        it.`package` = BeepizBindingConstants.packageName
    }
    try {
        withBoundService(intent) { serviceBinder ->
            val incomingChannel = Channel<Pair<BeepizMonitoringState, Boolean>>(
                capacity = Channel.CONFLATED
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
            val service = Messenger(serviceBinder)
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
    } catch (ignored: DeadObjectException) { // Remote process died. We can only retry.
    }
}
