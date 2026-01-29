package com.v2ray.hwidkit

import java.util.Locale

enum class UserAgentPreset(val key: String) {
    AUTO("auto"),
    HAPP("happ"),
    HAPP_3_8_1("happ_3_8_1"),
    V2RAYNG("v2rayng"),
    V2RAYTUN("v2raytun"),
    FLCLASHX("flclashx"),
    CUSTOM("custom");

    val isHapp: Boolean
        get() = this == HAPP || this == HAPP_3_8_1

    companion object {
        fun fromKey(value: String?): UserAgentPreset {
            val normalized = value?.trim()?.lowercase(Locale.US).orEmpty()
            return entries.firstOrNull { it.key == normalized } ?: AUTO
        }
    }
}
