package com.example.beepizcontrol.extensions.android.content.pm

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build.VERSION.SDK_INT
import android.os.PatternMatcher
import com.example.beepizcontrol.BeepizBindingConstants
import com.example.beepizcontrol.extensions.android.content.broadcastReceiverChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.withContext

suspend fun Context.awaitPackageInstalled(packageName: String, minimumVersionCode: Long = 0) {
    withContext(Dispatchers.IO) {
        if (packageManager.isPackageInstalled(packageName, minimumVersionCode)) {
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
            if (packageManager.isPackageInstalled(packageName, minimumVersionCode)) {
                return@withContext // Just installed/updated after we registered.
            }
            receive()
            return@withContext
        }
    }
}

fun PackageManager.isPackageInstalled(
    packageName: String,
    minimumVersionCode: Long = 0
): Boolean = try {
    val longVersionCode = getPackageInfo(packageName, 0).let {
        if (SDK_INT >= 28) it.longVersionCode else {
            @Suppress("DEPRECATION")
            it.versionCode.toLong()
        }
    }
    longVersionCode >= minimumVersionCode
} catch (e: PackageManager.NameNotFoundException) {
    false
}
