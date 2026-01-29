package com.v2ray.hwidkit

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.tencent.mmkv.MMKV
import java.net.HttpURLConnection
import java.util.Locale

object HwidKit {

    private object V2rayNgPrefKeys {
        const val PREF_HWID_ENABLED = "pref_hwid_enabled"
        const val PREF_HWID_VAL = "pref_hwid_val"
        const val PREF_HWID_OS = "pref_hwid_os"
        const val PREF_HWID_OS_VER = "pref_hwid_os_ver"
        const val PREF_HWID_MODEL = "pref_hwid_model"
        const val PREF_HWID_LOCALE = "pref_hwid_locale"
        const val PREF_HWID_USER_AGENT = "pref_hwid_user_agent"
        const val PREF_HWID_USER_AGENT_PRESET = "pref_hwid_user_agent_preset"
        const val PREF_HWID_V2RAYTUN_PLATFORM = "pref_hwid_v2raytun_platform"
        const val PREF_HWID_FLCLASHX_PLATFORM = "pref_hwid_flclashx_platform"
        const val PREF_HWID_USER_AGENT_HAPP_VERSION = "pref_hwid_user_agent_happ_version"
        const val PREF_HWID_USER_AGENT_V2RAYNG_VERSION = "pref_hwid_user_agent_v2rayng_version"
        const val PREF_HWID_USER_AGENT_FLCLASHX_VERSION = "pref_hwid_user_agent_flclashx_version"
    }

    fun resolveUserAgent(
        config: HwidConfig,
        subscriptionUserAgent: String?,
        defaultUserAgent: String,
    ): String {
        if (!subscriptionUserAgent.isNullOrBlank()) return subscriptionUserAgent

        if (!config.enabled) return defaultUserAgent

        val preset = config.userAgentPreset.trim().ifEmpty { "auto" }

        val presetUa = when (preset) {
            "happ_3_8_1" -> "Happ/3.8.1"
            "happ" -> {
                val v = config.happVersion?.trim().orEmpty()
                if (v.isEmpty()) "Happ" else "Happ/$v"
            }
            "v2rayng" -> {
                val v = config.v2rayngVersion?.trim().orEmpty()
                if (v.isEmpty()) "v2rayNG" else "v2rayNG/$v"
            }
            "v2raytun" -> {
                val p = config.v2raytunPlatform?.trim().orEmpty().ifEmpty { "android" }
                "v2raytun/$p"
            }
            "flclashx" -> {
                val p = config.flclashxPlatform?.trim().orEmpty().ifEmpty { "android" }
                val v = config.flclashxVersion?.trim().orEmpty()
                if (v.isEmpty()) {
                    "FlClash X Platform/$p"
                } else {
                    "FlClash X/v$v Platform/$p"
                }
            }
            "custom" -> config.customUserAgent?.takeIf { it.isNotBlank() }
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
        val config = loadHwidConfigFromV2rayNgSettings(appVersionName)
            ?: HwidConfig(enabled = false)

        applyToConnection(
            conn = conn,
            context = context,
            config = config,
            subscriptionUserAgent = subscriptionUserAgent,
            defaultUserAgent = defaultUserAgent,
        )
    }

    private fun loadHwidConfigFromV2rayNgSettings(appVersionName: String): HwidConfig? {
        return try {
            val storage = MMKV.mmkvWithID("SETTING", MMKV.MULTI_PROCESS_MODE)
            val enabled = storage.decodeBool(V2rayNgPrefKeys.PREF_HWID_ENABLED, false)

            HwidConfig(
                enabled = enabled,
                customHwid = storage.decodeString(V2rayNgPrefKeys.PREF_HWID_VAL),
                customOs = storage.decodeString(V2rayNgPrefKeys.PREF_HWID_OS),
                customOsVersion = storage.decodeString(V2rayNgPrefKeys.PREF_HWID_OS_VER),
                customLocale = storage.decodeString(V2rayNgPrefKeys.PREF_HWID_LOCALE),
                customModel = storage.decodeString(V2rayNgPrefKeys.PREF_HWID_MODEL),
                userAgentPreset = if (enabled) {
                    storage.decodeString(V2rayNgPrefKeys.PREF_HWID_USER_AGENT_PRESET, "auto") ?: "auto"
                } else {
                    "auto"
                },
                customUserAgent = storage.decodeString(V2rayNgPrefKeys.PREF_HWID_USER_AGENT),
                happVersion = storage.decodeString(V2rayNgPrefKeys.PREF_HWID_USER_AGENT_HAPP_VERSION, "3.8.1"),
                v2rayngVersion = storage.decodeString(V2rayNgPrefKeys.PREF_HWID_USER_AGENT_V2RAYNG_VERSION, appVersionName),
                v2raytunPlatform = storage.decodeString(V2rayNgPrefKeys.PREF_HWID_V2RAYTUN_PLATFORM, "android"),
                flclashxVersion = storage.decodeString(V2rayNgPrefKeys.PREF_HWID_USER_AGENT_FLCLASHX_VERSION, "0.3.0"),
                flclashxPlatform = storage.decodeString(V2rayNgPrefKeys.PREF_HWID_FLCLASHX_PLATFORM, "android"),
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun hardwareId(context: Context): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        } catch (_: Exception) {
            ""
        }
    }

    private fun deviceOsValue(): String = "android"

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
