package com.v2ray.devicekit

import com.tencent.mmkv.MMKV

internal object SettingsStore {
    private const val MMKV_ID = "SETTING"

    fun loadConfig(appVersionName: String): Config? {
        return try {
            val storage = MMKV.mmkvWithID(MMKV_ID, MMKV.MULTI_PROCESS_MODE)
            val enabled = storage.decodeBool(PrefKeys.HWID_ENABLED, false)
            val preset = if (enabled) {
                UserAgentPreset.fromKey(storage.decodeString(PrefKeys.UA_PRESET))
            } else {
                UserAgentPreset.AUTO
            }

            Config(
                enabled = enabled,
                customHwid = storage.decodeString(PrefKeys.HWID_VAL),
                customOs = storage.decodeString(PrefKeys.HWID_OS),
                customOsVersion = storage.decodeString(PrefKeys.HWID_OS_VER),
                customLocale = storage.decodeString(PrefKeys.HWID_LOCALE),
                customModel = storage.decodeString(PrefKeys.HWID_MODEL),
                userAgentPreset = preset,
                customUserAgent = storage.decodeString(PrefKeys.UA_CUSTOM),
                happVersion = storage.decodeString(PrefKeys.UA_HAPP_VERSION, Defaults.HAPP_VERSION),
                v2rayngVersion = storage.decodeString(PrefKeys.UA_V2RAYNG_VERSION, appVersionName),
                v2raytunPlatform = storage.decodeString(PrefKeys.UA_V2RAYTUN_PLATFORM, Defaults.V2RAYTUN_PLATFORM),
                flclashxVersion = storage.decodeString(PrefKeys.UA_FLCLASHX_VERSION, Defaults.FLCLASHX_VERSION),
                flclashxPlatform = storage.decodeString(PrefKeys.UA_FLCLASHX_PLATFORM, Defaults.FLCLASHX_PLATFORM),
            )
        } catch (_: Exception) {
            null
        }
    }
}
