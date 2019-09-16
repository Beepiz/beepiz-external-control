package com.example.beepizcontrol

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.beepizcontrol.BeepizMonitoringState.MONITORING
import com.example.beepizcontrol.BeepizMonitoringState.NOT_MONITORING
import com.example.beepizcontrol.extensions.android.permissions.ensurePermissionOrFinishAndCancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ui = BeepizControlUiImpl(this)
        setContentView(ui.root)
        launch {
            ensurePermissionOrFinishAndCancel(
                permission = BeepizBindingConstants.permission,
                askDialogTitle = null,
                askDialogMessage = "Permission required",
                showRationaleBeforeFirstAsk = false
            )
            @UseExperimental(ExperimentalCoroutinesApi::class)
            beepizStateFlow().collectLatest { (state, requiresConfig) ->
                when (state) {
                    MONITORING -> {
                        ui.awaitStopMonitoringRequest()
                        if (requiresConfig) {
                            launchBeepizForConfiguration()
                        } else stopBeepizMonitoring()
                    }
                    NOT_MONITORING -> {
                        ui.awaitStartMonitoringRequest()
                        if (requiresConfig) {
                            launchBeepizForConfiguration()
                        } else startBeepizMonitoring()
                    }
                }
            }
        }
    }

    private fun launchBeepizForConfiguration() {
        packageManager.getLaunchIntentForPackage(BeepizBindingConstants.packageName)?.let {
            startActivity(it)
        }
    }

    override fun onDestroy() {
        cancel() // Cancels the whole CoroutineScope, which prevents leaks.
        super.onDestroy()
    }
}
