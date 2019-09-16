package com.example.beepizcontrol

import android.content.Context
import androidx.annotation.RequiresPermission
import com.example.beepizcontrol.extensions.android.content.pm.awaitPackageInstalled
import com.example.beepizcontrol.extensions.coroutines.repeatWhileActive
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow


data class BeepizState(
    val monitoringState: BeepizMonitoringState,
    val requiresConfig: Boolean
)

@RequiresPermission(BeepizBindingConstants.permission)
fun Context.beepizStateFlow(): Flow<BeepizState> {
    return flow {
        repeatWhileActive {
            awaitBeepizInstalled()
            runBeepizBindingUntilDisconnection { state, requiresConfig ->
                emit(BeepizState(state, requiresConfig))
            }
        }
    }.run {
        @UseExperimental(ExperimentalCoroutinesApi::class)
        conflate()
    }
}

private suspend inline fun Context.awaitBeepizInstalled() {
    awaitPackageInstalled(BeepizBindingConstants.packageName, minimumVersionCode = 165)
}
