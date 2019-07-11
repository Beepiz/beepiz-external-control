package com.example.beepizcontrol.extensions.android.view

import android.view.View
import androidx.core.view.isVisible
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Awaits a click to be performed on this [View], then resumes.
 * This function is cancellable.
 *
 * By default, [disableAfterClick] is set to `true`, and that will make this View to be enabled
 * when this function is invoked, and finally be disabled back, after click or cancellation.
 * Note that if you set this parameter to `false`, no attempt to enable the button will be made.
 *
 * The [hideAfterClick] parameter is set to `false` by default.
 *
 * If enabled, the view will be made
 * visible when this function is invoked, and finally be hidden back (visibility to [View.GONE]),
 * after click or cancellation. Note that **no transition will be performed**.
 */
suspend fun View.awaitOneClick(
    disableAfterClick: Boolean = true,
    hideAfterClick: Boolean = false
) = try {
    if (disableAfterClick) isEnabled = true
    if (hideAfterClick) isVisible = true
    suspendCancellableCoroutine<Unit> { continuation ->
        setOnClickListener {
            setOnClickListener(null)
            continuation.resume(Unit)
        }
    }
} finally {
    setOnClickListener(null)
    if (disableAfterClick) isEnabled = false
    if (hideAfterClick) isVisible = false
}
