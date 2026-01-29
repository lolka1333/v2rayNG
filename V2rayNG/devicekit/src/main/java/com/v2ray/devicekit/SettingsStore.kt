package com.v2ray.hwidkit

import com.tencent.mmkv.MMKV

internal object HwidSettingsStore {
    private const val MMKV_ID = "SETTING"

    fun loadHwidConfig(appVersionName: String): HwidConfig? {
        return try {
            val storage = MMKV.mmkvWithID(MMKV_ID, MMKV.MULTI_PROCESS_MODE)
            val enabled = storage.decodeBool(HwidPreferenceKeys.HWID_ENABLED, false)
            val preset = if (enabled) {
                UserAgentPreset.fromKey(storage.decodeString(HwidPreferenceKeys.UA_PRESET))
            } else {
                UserAgentPreset.AUTO
            }

            HwidConfig(
                enabled = enabled,
                customHwid = storage.decodeString(HwidPreferenceKeys.HWID_VAL),
                customOs = storage.decodeString(HwidPreferenceKeys.HWID_OS),
                customOsVersion = storage.decodeString(HwidPreferenceKeys.HWID_OS_VER),
                customLocale = storage.decodeString(HwidPreferenceKeys.HWID_LOCALE),
                customModel = storage.decodeString(HwidPreferenceKeys.HWID_MODEL),
                userAgentPreset = preset,
                customUserAgent = storage.decodeString(HwidPreferenceKeys.UA_CUSTOM),
                happVersion = storage.decodeString(HwidPreferenceKeys.UA_HAPP_VERSION, HwidDefaults.HAPP_VERSION),
                v2rayngVersion = storage.decodeString(HwidPreferenceKeys.UA_V2RAYNG_VERSION, appVersionName),
                v2raytunPlatform = storage.decodeString(HwidPreferenceKeys.UA_V2RAYTUN_PLATFORM, HwidDefaults.V2RAYTUN_PLATFORM),
                flclashxVersion = storage.decodeString(HwidPreferenceKeys.UA_FLCLASHX_VERSION, HwidDefaults.FLCLASHX_VERSION),
                flclashxPlatform = storage.decodeString(HwidPreferenceKeys.UA_FLCLASHX_PLATFORM, HwidDefaults.FLCLASHX_PLATFORM),
            )
        } catch (_: Exception) {
            null
        }
    }
}
