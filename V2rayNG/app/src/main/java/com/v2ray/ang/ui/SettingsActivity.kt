package com.v2ray.ang.ui

import android.os.Bundle
import android.view.View
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.multiprocess.RemoteWorkManager
import com.v2ray.ang.AngApplication
import com.v2ray.ang.AppConfig
import com.v2ray.ang.AppConfig.VPN
import com.v2ray.ang.R
import com.v2ray.ang.extension.toLongEx
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.handler.MmkvPreferenceDataStore
import com.v2ray.ang.handler.SubscriptionUpdater
import com.v2ray.ang.util.Utils
import java.util.concurrent.TimeUnit
import java.util.Locale
import java.util.UUID

class SettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentViewWithToolbar(R.layout.activity_settings, showHomeAsUp = true, title = getString(R.string.title_settings))
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        private val localDns by lazy { findPreference<CheckBoxPreference>(AppConfig.PREF_LOCAL_DNS_ENABLED) }
        private val fakeDns by lazy { findPreference<CheckBoxPreference>(AppConfig.PREF_FAKE_DNS_ENABLED) }
        private val appendHttpProxy by lazy { findPreference<CheckBoxPreference>(AppConfig.PREF_APPEND_HTTP_PROXY) }

        //        private val localDnsPort by lazy { findPreference<EditTextPreference>(AppConfig.PREF_LOCAL_DNS_PORT) }
        private val vpnDns by lazy { findPreference<EditTextPreference>(AppConfig.PREF_VPN_DNS) }
        private val vpnBypassLan by lazy { findPreference<ListPreference>(AppConfig.PREF_VPN_BYPASS_LAN) }
        private val vpnInterfaceAddress by lazy { findPreference<ListPreference>(AppConfig.PREF_VPN_INTERFACE_ADDRESS_CONFIG_INDEX) }
        private val vpnMtu by lazy { findPreference<EditTextPreference>(AppConfig.PREF_VPN_MTU) }

        private val mux by lazy { findPreference<CheckBoxPreference>(AppConfig.PREF_MUX_ENABLED) }
        private val muxConcurrency by lazy { findPreference<EditTextPreference>(AppConfig.PREF_MUX_CONCURRENCY) }
        private val muxXudpConcurrency by lazy { findPreference<EditTextPreference>(AppConfig.PREF_MUX_XUDP_CONCURRENCY) }
        private val muxXudpQuic by lazy { findPreference<ListPreference>(AppConfig.PREF_MUX_XUDP_QUIC) }

        private val fragment by lazy { findPreference<CheckBoxPreference>(AppConfig.PREF_FRAGMENT_ENABLED) }
        private val fragmentPackets by lazy { findPreference<ListPreference>(AppConfig.PREF_FRAGMENT_PACKETS) }
        private val fragmentLength by lazy { findPreference<EditTextPreference>(AppConfig.PREF_FRAGMENT_LENGTH) }
        private val fragmentInterval by lazy { findPreference<EditTextPreference>(AppConfig.PREF_FRAGMENT_INTERVAL) }

        private val autoUpdateCheck by lazy { findPreference<CheckBoxPreference>(AppConfig.SUBSCRIPTION_AUTO_UPDATE) }
        private val autoUpdateInterval by lazy { findPreference<EditTextPreference>(AppConfig.SUBSCRIPTION_AUTO_UPDATE_INTERVAL) }
        private val mode by lazy { findPreference<ListPreference>(AppConfig.PREF_MODE) }

        override fun onCreatePreferences(bundle: Bundle?, s: String?) {
            // Use MMKV as the storage backend for all Preferences
            // This prevents inconsistencies between SharedPreferences and MMKV
            preferenceManager.preferenceDataStore = MmkvPreferenceDataStore()

            addPreferencesFromResource(R.xml.pref_settings)

            initPreferenceSummaries()

            localDns?.setOnPreferenceChangeListener { _, any ->
                updateLocalDns(any as Boolean)
                true
            }

            mux?.setOnPreferenceChangeListener { _, newValue ->
                updateMux(newValue as Boolean)
                true
            }
            muxConcurrency?.setOnPreferenceChangeListener { _, newValue ->
                updateMuxConcurrency(newValue as String)
                true
            }
            muxXudpConcurrency?.setOnPreferenceChangeListener { _, newValue ->
                updateMuxXudpConcurrency(newValue as String)
                true
            }

            fragment?.setOnPreferenceChangeListener { _, newValue ->
                updateFragment(newValue as Boolean)
                true
            }

            autoUpdateCheck?.setOnPreferenceChangeListener { _, newValue ->
                val value = newValue as Boolean
                autoUpdateCheck?.isChecked = value
                autoUpdateInterval?.isEnabled = value
                autoUpdateInterval?.text?.toLongEx()?.let {
                    if (newValue) configureUpdateTask(it) else cancelUpdateTask()
                }
                true
            }
            mode?.setOnPreferenceChangeListener { _, newValue ->
                updateMode(newValue.toString())
                true
            }
            mode?.dialogLayoutResource = R.layout.preference_with_help_link

            val hwidUaPresetPref = findPreference<ListPreference>(AppConfig.PREF_HWID_USER_AGENT_PRESET)
            val hwidUaPref = findPreference<EditTextPreference>(AppConfig.PREF_HWID_USER_AGENT)
            val hwidUaHappVerPref = findPreference<EditTextPreference>(AppConfig.PREF_HWID_USER_AGENT_HAPP_VERSION)
            val hwidUaV2rayngVerPref = findPreference<EditTextPreference>(AppConfig.PREF_HWID_USER_AGENT_V2RAYNG_VERSION)
            val hwidUaFlclashxVerPref = findPreference<EditTextPreference>(AppConfig.PREF_HWID_USER_AGENT_FLCLASHX_VERSION)
            fun syncHwidUaUiState() {
                val enabled = MmkvManager.decodeSettingsBool(AppConfig.PREF_HWID_ENABLED, false)
                val preset = hwidUaPresetPref?.value ?: MmkvManager.decodeSettingsString(AppConfig.PREF_HWID_USER_AGENT_PRESET, "auto")
                val showCustomUa = enabled && preset == "custom"
                val showHappVer = enabled && preset == "happ"
                val showV2rayngVer = enabled && preset == "v2rayng"
                val showFlclashxVer = enabled && preset == "flclashx"

                hwidUaPref?.isVisible = showCustomUa
                hwidUaPref?.isEnabled = showCustomUa

                hwidUaHappVerPref?.isVisible = showHappVer
                hwidUaHappVerPref?.isEnabled = showHappVer

                hwidUaV2rayngVerPref?.isVisible = showV2rayngVer
                hwidUaV2rayngVerPref?.isEnabled = showV2rayngVer

                hwidUaFlclashxVerPref?.isVisible = showFlclashxVer
                hwidUaFlclashxVerPref?.isEnabled = showFlclashxVer
            }

            hwidUaPresetPref?.setOnPreferenceChangeListener { _, newValue ->
                // ListPreference summary will be updated by initPreferenceSummaries
                val enabled = MmkvManager.decodeSettingsBool(AppConfig.PREF_HWID_ENABLED, false)
                val preset = newValue?.toString().orEmpty()
                val idx = hwidUaPresetPref.findIndexOfValue(preset)
                hwidUaPresetPref.summary = (if (idx >= 0) hwidUaPresetPref.entries[idx] else preset) as CharSequence?
                val showCustomUa = enabled && preset == "custom"
                val showHappVer = enabled && preset == "happ"
                val showV2rayngVer = enabled && preset == "v2rayng"
                val showFlclashxVer = enabled && preset == "flclashx"

                hwidUaPref?.isVisible = showCustomUa
                hwidUaPref?.isEnabled = showCustomUa

                hwidUaHappVerPref?.isVisible = showHappVer
                hwidUaHappVerPref?.isEnabled = showHappVer

                hwidUaV2rayngVerPref?.isVisible = showV2rayngVer
                hwidUaV2rayngVerPref?.isEnabled = showV2rayngVer

                hwidUaFlclashxVerPref?.isVisible = showFlclashxVer
                hwidUaFlclashxVerPref?.isEnabled = showFlclashxVer
                true
            }
            syncHwidUaUiState()

            findPreference<CheckBoxPreference>(AppConfig.PREF_HWID_ENABLED)?.setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                if (enabled) {
                    // Execute initialization on the next UI frame to avoid conflicts with Preference dependency updates
                    view?.post {
                        try {
                            val hwidPref = findPreference<androidx.preference.Preference>(AppConfig.PREF_HWID_VAL) as? EditTextPreference
                            if (hwidPref != null && hwidPref.text.isNullOrEmpty()) {
                                val generatedHwid = UUID.randomUUID().toString().replace("-", "")
                                hwidPref.text = generatedHwid
                                hwidPref.summary = generatedHwid
                                MmkvManager.encodeSettings(AppConfig.PREF_HWID_VAL, generatedHwid)
                            }

                            val osPref = findPreference<androidx.preference.Preference>(AppConfig.PREF_HWID_OS) as? ListPreference
                            if (osPref != null && osPref.value == null) {
                                val defaultOS = Utils.getDeviceOS()
                                osPref.value = defaultOS
                                osPref.summary = defaultOS
                                MmkvManager.encodeSettings(AppConfig.PREF_HWID_OS, defaultOS)
                            }

                            val osVerPref = findPreference<androidx.preference.Preference>(AppConfig.PREF_HWID_OS_VER) as? EditTextPreference
                            if (osVerPref != null && osVerPref.text.isNullOrEmpty()) {
                                val defaultVer = android.os.Build.VERSION.RELEASE
                                osVerPref.text = defaultVer
                                osVerPref.summary = defaultVer
                                MmkvManager.encodeSettings(AppConfig.PREF_HWID_OS_VER, defaultVer)
                            }

                            val modelPref = findPreference<androidx.preference.Preference>(AppConfig.PREF_HWID_MODEL) as? EditTextPreference
                            if (modelPref != null && modelPref.text.isNullOrEmpty()) {
                                val defaultModel = android.os.Build.MODEL
                                modelPref.text = defaultModel
                                modelPref.summary = defaultModel
                                MmkvManager.encodeSettings(AppConfig.PREF_HWID_MODEL, defaultModel)
                            }

                            val localePref = findPreference<androidx.preference.Preference>(AppConfig.PREF_HWID_LOCALE) as? EditTextPreference
                            if (localePref != null && localePref.text.isNullOrEmpty()) {
                                val defaultLocale = Locale.getDefault().language
                                localePref.text = defaultLocale
                                localePref.summary = defaultLocale
                                MmkvManager.encodeSettings(AppConfig.PREF_HWID_LOCALE, defaultLocale)
                            }

                            val uaPref = findPreference<androidx.preference.Preference>(AppConfig.PREF_HWID_USER_AGENT) as? EditTextPreference
                            if (uaPref != null && uaPref.text.isNullOrEmpty()) {
                                val defaultUa = "v2rayNG/${com.v2ray.ang.BuildConfig.VERSION_NAME}"
                                uaPref.text = defaultUa
                                uaPref.summary = defaultUa
                                MmkvManager.encodeSettings(AppConfig.PREF_HWID_USER_AGENT, defaultUa)
                            }

                            val uaPresetPref = findPreference<androidx.preference.Preference>(AppConfig.PREF_HWID_USER_AGENT_PRESET) as? ListPreference
                            if (uaPresetPref != null && uaPresetPref.value == null) {
                                uaPresetPref.value = "auto"
                                uaPresetPref.summary = uaPresetPref.entry ?: "auto"
                                MmkvManager.encodeSettings(AppConfig.PREF_HWID_USER_AGENT_PRESET, "auto")
                            }

                            val happVerPref = findPreference<androidx.preference.Preference>(AppConfig.PREF_HWID_USER_AGENT_HAPP_VERSION) as? EditTextPreference
                            if (happVerPref != null && happVerPref.text.isNullOrEmpty()) {
                                val defaultHappVer = "3.8.1"
                                happVerPref.text = defaultHappVer
                                happVerPref.summary = defaultHappVer
                                MmkvManager.encodeSettings(AppConfig.PREF_HWID_USER_AGENT_HAPP_VERSION, defaultHappVer)
                            }

                            val v2rayngVerPref = findPreference<androidx.preference.Preference>(AppConfig.PREF_HWID_USER_AGENT_V2RAYNG_VERSION) as? EditTextPreference
                            if (v2rayngVerPref != null && v2rayngVerPref.text.isNullOrEmpty()) {
                                val defaultV2rayngVer = com.v2ray.ang.BuildConfig.VERSION_NAME
                                v2rayngVerPref.text = defaultV2rayngVer
                                v2rayngVerPref.summary = defaultV2rayngVer
                                MmkvManager.encodeSettings(AppConfig.PREF_HWID_USER_AGENT_V2RAYNG_VERSION, defaultV2rayngVer)
                            }

                            val flclashxVerPref = findPreference<androidx.preference.Preference>(AppConfig.PREF_HWID_USER_AGENT_FLCLASHX_VERSION) as? EditTextPreference
                            if (flclashxVerPref != null && flclashxVerPref.text.isNullOrEmpty()) {
                                val defaultFlclashxVer = "0.3.0"
                                flclashxVerPref.text = defaultFlclashxVer
                                flclashxVerPref.summary = defaultFlclashxVer
                                MmkvManager.encodeSettings(AppConfig.PREF_HWID_USER_AGENT_FLCLASHX_VERSION, defaultFlclashxVer)
                            }

                            syncHwidUaUiState()
                        } catch (e: Exception) {
                            android.util.Log.e(AppConfig.TAG, "Error initializing HWID fields", e)
                        }
                    }
                }
                if (!enabled) {
                    syncHwidUaUiState()
                }
                true
            }

        }

        private fun initPreferenceSummaries() {
            fun updateSummary(pref: androidx.preference.Preference) {
                when (pref) {
                    is EditTextPreference -> {
                        pref.summary = pref.text.orEmpty()
                        pref.setOnPreferenceChangeListener { p, newValue ->
                            p.summary = (newValue as? String).orEmpty()
                            true
                        }
                    }

                    is ListPreference -> {
                        pref.summary = pref.entry ?: ""
                        pref.setOnPreferenceChangeListener { p, newValue ->
                            val lp = p as ListPreference
                            val idx = lp.findIndexOfValue(newValue as? String)
                            lp.summary = (if (idx >= 0) lp.entries[idx] else newValue) as CharSequence?
                            true
                        }
                    }

                    is CheckBoxPreference, is androidx.preference.SwitchPreferenceCompat -> {
                    }
                }
            }

            fun traverse(group: androidx.preference.PreferenceGroup) {
                for (i in 0 until group.preferenceCount) {
                    when (val p = group.getPreference(i)) {
                        is androidx.preference.PreferenceGroup -> traverse(p)
                        else -> updateSummary(p)
                    }
                }
            }

            preferenceScreen?.let { traverse(it) }
        }

        override fun onStart() {
            super.onStart()
            // Initialize mode-dependent UI states
            updateMode(MmkvManager.decodeSettingsString(AppConfig.PREF_MODE, VPN))

            // Initialize HWID user-agent UI state
            val hwidUaPresetPref = findPreference<ListPreference>(AppConfig.PREF_HWID_USER_AGENT_PRESET)
            val hwidUaPref = findPreference<EditTextPreference>(AppConfig.PREF_HWID_USER_AGENT)
            val hwidUaHappVerPref = findPreference<EditTextPreference>(AppConfig.PREF_HWID_USER_AGENT_HAPP_VERSION)
            val hwidUaV2rayngVerPref = findPreference<EditTextPreference>(AppConfig.PREF_HWID_USER_AGENT_V2RAYNG_VERSION)
            val hwidUaFlclashxVerPref = findPreference<EditTextPreference>(AppConfig.PREF_HWID_USER_AGENT_FLCLASHX_VERSION)
            val hwidEnabled = MmkvManager.decodeSettingsBool(AppConfig.PREF_HWID_ENABLED, false)
            val preset = hwidUaPresetPref?.value ?: MmkvManager.decodeSettingsString(AppConfig.PREF_HWID_USER_AGENT_PRESET, "auto")
            val showCustomUa = hwidEnabled && preset == "custom"
            val showHappVer = hwidEnabled && preset == "happ"
            val showV2rayngVer = hwidEnabled && preset == "v2rayng"
            val showFlclashxVer = hwidEnabled && preset == "flclashx"

            hwidUaPref?.isVisible = showCustomUa
            hwidUaPref?.isEnabled = showCustomUa

            hwidUaHappVerPref?.isVisible = showHappVer
            hwidUaHappVerPref?.isEnabled = showHappVer

            hwidUaV2rayngVerPref?.isVisible = showV2rayngVer
            hwidUaV2rayngVerPref?.isEnabled = showV2rayngVer

            hwidUaFlclashxVerPref?.isVisible = showFlclashxVer
            hwidUaFlclashxVerPref?.isEnabled = showFlclashxVer

            // Initialize mux-dependent UI states
            updateMux(MmkvManager.decodeSettingsBool(AppConfig.PREF_MUX_ENABLED, false))

            // Initialize fragment-dependent UI states
            updateFragment(MmkvManager.decodeSettingsBool(AppConfig.PREF_FRAGMENT_ENABLED, false))

            // Initialize auto-update interval state
            autoUpdateInterval?.isEnabled = MmkvManager.decodeSettingsBool(AppConfig.SUBSCRIPTION_AUTO_UPDATE, false)

        }

        private fun updateMode(mode: String?) {
            val vpn = mode == VPN
            localDns?.isEnabled = vpn
            fakeDns?.isEnabled = vpn
            appendHttpProxy?.isEnabled = vpn
//            localDnsPort?.isEnabled = vpn
            vpnDns?.isEnabled = vpn
            vpnBypassLan?.isEnabled = vpn
            vpnInterfaceAddress?.isEnabled = vpn
            vpnMtu?.isEnabled = vpn
            if (vpn) {
                updateLocalDns(
                    MmkvManager.decodeSettingsBool(
                        AppConfig.PREF_LOCAL_DNS_ENABLED,
                        false
                    )
                )
            }
        }

        private fun updateLocalDns(enabled: Boolean) {
            fakeDns?.isEnabled = enabled
//            localDnsPort?.isEnabled = enabled
            vpnDns?.isEnabled = !enabled
        }

        private fun configureUpdateTask(interval: Long) {
            val rw = RemoteWorkManager.getInstance(AngApplication.application)
            rw.cancelUniqueWork(AppConfig.SUBSCRIPTION_UPDATE_TASK_NAME)
            rw.enqueueUniquePeriodicWork(
                AppConfig.SUBSCRIPTION_UPDATE_TASK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequest.Builder(
                    SubscriptionUpdater.UpdateTask::class.java,
                    interval,
                    TimeUnit.MINUTES
                )
                    .apply {
                        setInitialDelay(interval, TimeUnit.MINUTES)
                    }
                    .build()
            )
        }

        private fun cancelUpdateTask() {
            val rw = RemoteWorkManager.getInstance(AngApplication.application)
            rw.cancelUniqueWork(AppConfig.SUBSCRIPTION_UPDATE_TASK_NAME)
        }

        private fun updateMux(enabled: Boolean) {
            muxConcurrency?.isEnabled = enabled
            muxXudpConcurrency?.isEnabled = enabled
            muxXudpQuic?.isEnabled = enabled
            if (enabled) {
                updateMuxConcurrency(MmkvManager.decodeSettingsString(AppConfig.PREF_MUX_CONCURRENCY, "8"))
                updateMuxXudpConcurrency(MmkvManager.decodeSettingsString(AppConfig.PREF_MUX_XUDP_CONCURRENCY, "8"))
            }
        }

        private fun updateMuxConcurrency(value: String?) {
            val concurrency = value?.toIntOrNull() ?: 8
            muxConcurrency?.summary = concurrency.toString()
        }


        private fun updateMuxXudpConcurrency(value: String?) {
            if (value == null) {
                muxXudpQuic?.isEnabled = true
            } else {
                val concurrency = value.toIntOrNull() ?: 8
                muxXudpConcurrency?.summary = concurrency.toString()
                muxXudpQuic?.isEnabled = concurrency >= 0
            }
        }

        private fun updateFragment(enabled: Boolean) {
            fragmentPackets?.isEnabled = enabled
            fragmentLength?.isEnabled = enabled
            fragmentInterval?.isEnabled = enabled
        }
    }

    fun onModeHelpClicked(view: View) {
        Utils.openUri(this, AppConfig.APP_WIKI_MODE)
    }
}
