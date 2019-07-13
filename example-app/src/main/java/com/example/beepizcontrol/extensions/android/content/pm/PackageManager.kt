package com.example.beepizcontrol.extensions.android.content.pm

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.PatternMatcher
import com.example.beepizcontrol.BeepizBindingConstants
import com.example.beepizcontrol.extensions.android.content.broadcastReceiverChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.withContext

suspend fun Context.awaitPackageInstalled(packageName: String) {
    withContext(Dispatchers.IO) {
        if (packageManager.isPackageInstalled(packageName)) {
            return@withContext // Fast path, app is already installed.
        }
        val channel = broadcastReceiverChannel(
            filter = IntentFilter(Intent.ACTION_PACKAGE_ADDED).apply {
                addDataScheme("package")
                addDataSchemeSpecificPart(packageName, PatternMatcher.PATTERN_LITERAL)
            },
            capacity = Channel.CONFLATED
        )
        @UseExperimental(ExperimentalCoroutinesApi::class)
        channel.consume {
            if (packageManager.isPackageInstalled(packageName)) {
                return@withContext // Just installed/updated after we registered.
            }
            receive()
            return@withContext
        }
    }
}

fun PackageManager.isPackageInstalled(packageName: String): Boolean = try {
    getPackageInfo(packageName, 0)
    true
} catch (e: PackageManager.NameNotFoundException) {
    false
}
