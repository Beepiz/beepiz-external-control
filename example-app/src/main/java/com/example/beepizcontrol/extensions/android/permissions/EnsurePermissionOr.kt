package com.example.beepizcontrol.extensions.android.permissions

import androidx.fragment.app.FragmentActivity
import com.example.beepizcontrol.R
import kotlinx.coroutines.suspendCancellableCoroutine
import splitties.experimental.ExperimentalSplittiesApi

@UseExperimental(ExperimentalSplittiesApi::class)
suspend fun FragmentActivity.ensurePermissionOrFinishAndCancel(
    permission: String,
    askDialogTitle: CharSequence?,
    askDialogMessage: CharSequence?,
    showRationaleBeforeFirstAsk: Boolean = true,
    returnButtonText: CharSequence = getText(R.string.quit)
): Unit = ensurePermission(
    activity = this,
    fragmentManager = supportFragmentManager,
    lifecycle = lifecycle,
    permission = permission,
    askDialogTitle = askDialogTitle,
    askDialogMessage = askDialogMessage,
    showRationaleBeforeFirstAsk = showRationaleBeforeFirstAsk,
    returnButtonText = returnButtonText
) {
    finish()
    suspendCancellableCoroutine<Nothing> { c -> c.cancel() } // Ensures the scope is cancelled.
}
