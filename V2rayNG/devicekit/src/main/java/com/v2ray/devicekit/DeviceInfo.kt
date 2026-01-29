package com.v2ray.devicekit

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import java.util.Locale

internal object DeviceInfo {
    fun hardwareId(context: Context): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    fun osValue(): String = Defaults.OS_VALUE_ANDROID

    fun osVersion(): String = Build.VERSION.RELEASE.orEmpty()

    fun model(): String {
        return try {
            Build.MODEL?.ifEmpty { "Unknown" } ?: "Unknown"
        } catch (_: Exception) {
            "Unknown"
        }
    }

    fun locale(): String = Locale.getDefault().language.orEmpty()

    fun appVersionName(context: Context): String {
        return try {
            val pm = context.packageManager
            val pkg = context.packageName
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, 0)
            }
            info.versionName.orEmpty()
        } catch (_: Exception) {
            ""
        }
    }
}
