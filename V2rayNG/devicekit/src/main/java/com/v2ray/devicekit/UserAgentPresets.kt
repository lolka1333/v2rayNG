package com.v2ray.devicekit

import java.util.Locale

enum class UserAgentPreset(val key: String) {
    AUTO("auto"),
    HAPP("happ"),
    V2RAYNG("v2rayng"),
    V2RAYTUN("v2raytun"),
    FLCLASHX("flclashx"),
    CUSTOM("custom");

    val isHapp: Boolean
        get() = this == HAPP

    companion object {
        fun fromKey(value: String?): UserAgentPreset {
            return entries.firstOrNull {
                it.key == value?.trim()?.lowercase(Locale.US).orEmpty()
            } ?: AUTO
        }
    }
}
