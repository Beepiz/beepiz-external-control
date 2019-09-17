package com.example.beepizcontrol

import android.content.Context
import androidx.annotation.RequiresPermission
import com.example.beepizcontrol.extensions.android.content.pm.awaitPackageInstalled
import com.example.beepizcontrol.extensions.coroutines.offerCatching
import com.example.beepizcontrol.extensions.coroutines.repeatWhileActive
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

data class BeepizState(
    val monitoringState: BeepizMonitoringState,
    val requiresConfig: Boolean
)

@RequiresPermission(BeepizBindingConstants.permission)
fun Context.beepizStateFlow(): Flow<BeepizState> {
    @UseExperimental(ExperimentalCoroutinesApi::class)
    return callbackFlow<BeepizState> {
        repeatWhileActive {
            awaitBeepizInstalled()
            runBeepizBindingUntilDisconnection { state, requiresConfig ->
                offerCatching(BeepizState(state, requiresConfig))
            }
        }
    }.conflate()
}

private suspend inline fun Context.awaitBeepizInstalled() {
    awaitPackageInstalled(BeepizBindingConstants.packageName, minimumVersionCode = 165)
}
