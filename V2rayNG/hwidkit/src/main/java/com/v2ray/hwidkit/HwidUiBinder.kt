package com.v2ray.hwidkit

import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat

object HwidUiBinder {

    private data class Prefs(
        val hwidEnabled: CheckBoxPreference?,
        val hwidVal: EditTextPreference?,
        val hwidOs: ListPreference?,
        val hwidOsVer: EditTextPreference?,
        val hwidModel: EditTextPreference?,
        val hwidLocale: EditTextPreference?,
        val uaPreset: ListPreference?,
        val uaCustom: EditTextPreference?,
        val uaHappVersion: EditTextPreference?,
        val uaV2rayngVersion: EditTextPreference?,
        val uaV2raytunPlatform: ListPreference?,
        val uaFlclashxVersion: EditTextPreference?,
        val uaFlclashxPlatform: ListPreference?,
    )

    fun bind(fragment: PreferenceFragmentCompat) {
        val prefs = prefs(fragment)

        prefs.hwidEnabled?.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as? Boolean ?: false
            updateVisibility(prefs, enabled, prefs.uaPreset?.value)
            true
        }

        prefs.uaPreset?.setOnPreferenceChangeListener { pref, newValue ->
            val lp = pref as ListPreference
            val valueStr = newValue?.toString().orEmpty()
            updateUaPresetSummary(lp, valueStr)
            updateVisibility(prefs, prefs.hwidEnabled?.isChecked == true, valueStr)
            true
        }

        updateUaPresetSummary(prefs.uaPreset, prefs.uaPreset?.value)
        updateVisibility(prefs, prefs.hwidEnabled?.isChecked == true, prefs.uaPreset?.value)
    }

    private fun prefs(fragment: PreferenceFragmentCompat): Prefs {
        return Prefs(
            hwidEnabled = fragment.findPreference(HwidPreferenceKeys.HWID_ENABLED),
            hwidVal = fragment.findPreference(HwidPreferenceKeys.HWID_VAL),
            hwidOs = fragment.findPreference(HwidPreferenceKeys.HWID_OS),
            hwidOsVer = fragment.findPreference(HwidPreferenceKeys.HWID_OS_VER),
            hwidModel = fragment.findPreference(HwidPreferenceKeys.HWID_MODEL),
            hwidLocale = fragment.findPreference(HwidPreferenceKeys.HWID_LOCALE),
            uaPreset = fragment.findPreference(HwidPreferenceKeys.UA_PRESET),
            uaCustom = fragment.findPreference(HwidPreferenceKeys.UA_CUSTOM),
            uaHappVersion = fragment.findPreference(HwidPreferenceKeys.UA_HAPP_VERSION),
            uaV2rayngVersion = fragment.findPreference(HwidPreferenceKeys.UA_V2RAYNG_VERSION),
            uaV2raytunPlatform = fragment.findPreference(HwidPreferenceKeys.UA_V2RAYTUN_PLATFORM),
            uaFlclashxVersion = fragment.findPreference(HwidPreferenceKeys.UA_FLCLASHX_VERSION),
            uaFlclashxPlatform = fragment.findPreference(HwidPreferenceKeys.UA_FLCLASHX_PLATFORM),
        )
    }

    private fun updateUaPresetSummary(pref: ListPreference?, value: String?) {
        if (pref == null) return
        val valueStr = value?.toString().orEmpty()
        val idx = pref.findIndexOfValue(valueStr)
        pref.summary = if (idx >= 0) pref.entries[idx] else valueStr
    }

    private fun updateVisibility(prefs: Prefs, enabled: Boolean, preset: String?) {
        val showGroup = enabled

        prefs.hwidVal?.isVisible = showGroup
        prefs.hwidOs?.isVisible = showGroup
        prefs.hwidOsVer?.isVisible = showGroup
        prefs.hwidModel?.isVisible = showGroup
        prefs.hwidLocale?.isVisible = showGroup
        prefs.uaPreset?.isVisible = showGroup

        val normalized = UserAgentPreset.fromKey(preset)
        val showHapp = showGroup && normalized.isHapp
        val showV2rayng = showGroup && normalized == UserAgentPreset.V2RAYNG
        val showV2raytun = showGroup && normalized == UserAgentPreset.V2RAYTUN
        val showFlclashx = showGroup && normalized == UserAgentPreset.FLCLASHX
        val showCustom = showGroup && normalized == UserAgentPreset.CUSTOM

        prefs.uaHappVersion?.isVisible = showHapp
        prefs.uaV2rayngVersion?.isVisible = showV2rayng
        prefs.uaV2raytunPlatform?.isVisible = showV2raytun
        prefs.uaFlclashxPlatform?.isVisible = showFlclashx
        prefs.uaFlclashxVersion?.isVisible = showFlclashx
        prefs.uaCustom?.isVisible = showCustom
    }
}
