package com.example.beepizcontrol

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.beepizcontrol.BeepizMonitoringState.MONITORING
import com.example.beepizcontrol.BeepizMonitoringState.NOT_MONITORING
import com.example.beepizcontrol.extensions.android.permissions.ensurePermissionOrFinishAndCancel
import com.example.beepizcontrol.extensions.coroutines.repeatWhileActive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
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
            repeatWhileActive {
                //TODO: Handle Beepiz not installed, new installation, update and versionCode < 165.
                runBeepizBindingUntilDisconnection { state, requiresConfig ->
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
    }

    private fun launchBeepizForConfiguration() {
        startActivity(packageManager.getLaunchIntentForPackage(BeepizBindingConstants.packageName))
    }

    override fun onDestroy() {
        cancel() // Cancels the whole CoroutineScope, which prevents leaks.
        super.onDestroy()
    }
}