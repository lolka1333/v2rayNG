package com.v2ray.hwidkit

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.net.HttpURLConnection
import java.util.Locale

object HwidKit {

    fun resolveUserAgent(
        config: HwidConfig,
        subscriptionUserAgent: String?,
        defaultUserAgent: String,
    ): String {
        if (!subscriptionUserAgent.isNullOrBlank()) return subscriptionUserAgent

        if (!config.enabled) return defaultUserAgent

        val preset = config.userAgentPreset

        val presetUa = when (preset) {
            UserAgentPreset.HAPP -> {
                val v = config.happVersion?.trim().orEmpty().ifEmpty { HwidDefaults.HAPP_VERSION }
                "Happ/$v"
            }
            UserAgentPreset.V2RAYNG -> {
                val v = config.v2rayngVersion?.trim().orEmpty()
                if (v.isEmpty()) "v2rayNG" else "v2rayNG/$v"
            }
            UserAgentPreset.V2RAYTUN -> {
                val p = config.v2raytunPlatform?.trim().orEmpty().ifEmpty { HwidDefaults.V2RAYTUN_PLATFORM }
                "v2raytun/$p"
            }
            UserAgentPreset.FLCLASHX -> {
                val p = config.flclashxPlatform?.trim().orEmpty().ifEmpty { HwidDefaults.FLCLASHX_PLATFORM }
                val v = config.flclashxVersion?.trim().orEmpty()
                if (v.isEmpty()) {
                    "FlClash X Platform/$p"
                } else {
                    "FlClash X/v$v Platform/$p"
                }
            }
            UserAgentPreset.CUSTOM -> config.customUserAgent?.takeIf { it.isNotBlank() }
            else -> null
        }

        return presetUa ?: defaultUserAgent
    }

    fun applyToConnection(
        conn: HttpURLConnection,
        context: Context,
        config: HwidConfig,
        subscriptionUserAgent: String?,
        defaultUserAgent: String,
    ) {
        val finalUserAgent = resolveUserAgent(config, subscriptionUserAgent, defaultUserAgent)
        conn.setRequestProperty("User-agent", finalUserAgent)

        if (!config.enabled) return

        val hwidToSend = config.customHwid?.trim().takeIf { !it.isNullOrEmpty() } ?: hardwareId(context)
        if (hwidToSend.isNullOrEmpty()) return

        conn.setRequestProperty("X-HWID", hwidToSend)

        val osRaw = config.customOs?.trim().orEmpty().ifEmpty { deviceOsValue() }
        conn.setRequestProperty("X-Device-OS", hwidOsHeaderValue(osRaw))

        val osVer = config.customOsVersion?.trim().orEmpty().ifEmpty { Build.VERSION.RELEASE }
        conn.setRequestProperty("X-Ver-OS", osVer)

        val locale = config.customLocale?.trim().orEmpty().ifEmpty { Locale.getDefault().language }
        if (locale.isNotEmpty()) {
            conn.setRequestProperty("X-Device-Locale", locale)
        }

        val model = config.customModel?.trim().orEmpty().ifEmpty { deviceModel() }
        conn.setRequestProperty("X-Device-Model", model)
    }

    fun applyToConnectionFromV2rayNgSettings(
        conn: HttpURLConnection,
        context: Context,
        subscriptionUserAgent: String?,
        defaultUserAgent: String,
        appVersionName: String,
    ) {
        val config = HwidSettingsStore.loadHwidConfig(appVersionName)
            ?: HwidConfig(enabled = false)

        applyToConnection(
            conn = conn,
            context = context,
            config = config,
            subscriptionUserAgent = subscriptionUserAgent,
            defaultUserAgent = defaultUserAgent,
        )
    }

    private fun hardwareId(context: Context): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    private fun deviceOsValue(): String = HwidDefaults.OS_VALUE_ANDROID

    private fun deviceModel(): String {
        return try {
            Build.MODEL?.ifEmpty { "Unknown" } ?: "Unknown"
        } catch (_: Exception) {
            "Unknown"
        }
    }

    private fun hwidOsHeaderValue(os: String?): String {
        val v = os?.trim().orEmpty()
        if (v.isEmpty()) return "Android"

        return when (v.lowercase(Locale.US)) {
            "android" -> "Android"
            "ios" -> "iOS"
            "windows" -> "Windows"
            "macos" -> "macOS"
            "linux" -> "Linux"
            else -> v
        }
    }
}
