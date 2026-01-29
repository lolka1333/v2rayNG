package com.v2ray.devicekit

data class Config(
    val enabled: Boolean,
    val customHwid: String? = null,
    val customOs: String? = null,
    val customOsVersion: String? = null,
    val customLocale: String? = null,
    val customModel: String? = null,
    val userAgentPreset: UserAgentPreset = UserAgentPreset.AUTO,
    val customUserAgent: String? = null,
    val happVersion: String? = null,
    val v2rayngVersion: String? = null,
    val v2raytunPlatform: String? = null,
    val flclashxVersion: String? = null,
    val flclashxPlatform: String? = null,
)
