package com.ray.iptv.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class DataUsageState(
    val wifiRxBytes: Long = 0L,
    val wifiTxBytes: Long = 0L,
    val mobileRxBytes: Long = 0L,
    val mobileTxBytes: Long = 0L,
    val sessionBytes: Long = 0L,
    val isDataSaverEnabled: Boolean = false,
    val isMobileNetwork: Boolean = false
) {
    val totalWifiBytes: Long get() = wifiRxBytes + wifiTxBytes
    val totalMobileBytes: Long get() = mobileRxBytes + mobileTxBytes
    val totalAllBytes: Long get() = totalWifiBytes + totalMobileBytes
}

@Singleton
class DataUsageService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("ray_data_usage", Context.MODE_PRIVATE)
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _state = MutableStateFlow(DataUsageState())
    val state = _state.asStateFlow()

    private var baselineRx = -1L
    private var baselineTx = -1L
    private var sessionStartRx = -1L
    private var sessionStartTx = -1L

    companion object {
        private const val KEY_WIFI_RX = "ray_usage_wifi_rx"
        private const val KEY_WIFI_TX = "ray_usage_wifi_tx"
        private const val KEY_MOBILE_RX = "ray_usage_mobile_rx"
        private const val KEY_MOBILE_TX = "ray_usage_mobile_tx"
        private const val KEY_DATA_SAVER = "ray_data_saver_enabled"
    }

    init {
        loadSavedState()
        startPolling()
    }

    private fun loadSavedState() {
        val wRx = prefs.getLong(KEY_WIFI_RX, 0L)
        val wTx = prefs.getLong(KEY_WIFI_TX, 0L)
        val mRx = prefs.getLong(KEY_MOBILE_RX, 0L)
        val mTx = prefs.getLong(KEY_MOBILE_TX, 0L)
        val saver = prefs.getBoolean(KEY_DATA_SAVER, false)

        _state.value = DataUsageState(
            wifiRxBytes = wRx,
            wifiTxBytes = wTx,
            mobileRxBytes = mRx,
            mobileTxBytes = mTx,
            isDataSaverEnabled = saver,
            isMobileNetwork = checkIsMobile()
        )
    }

    private fun startPolling() {
        scope.launch {
            val myUid = Process.myUid()
            while (isActive) {
                val currentRx = TrafficStats.getUidRxBytes(myUid)
                val currentTx = TrafficStats.getUidTxBytes(myUid)

                if (currentRx != TrafficStats.UNSUPPORTED.toLong() && currentTx != TrafficStats.UNSUPPORTED.toLong()) {
                    if (sessionStartRx == -1L) {
                        sessionStartRx = currentRx
                        sessionStartTx = currentTx
                    }
                    if (baselineRx == -1L) {
                        baselineRx = currentRx
                        baselineTx = currentTx
                    } else {
                        val deltaRx = (currentRx - baselineRx).coerceAtLeast(0L)
                        val deltaTx = (currentTx - baselineTx).coerceAtLeast(0L)

                        if (deltaRx > 0 || deltaTx > 0) {
                            val isMobile = checkIsMobile()
                            if (isMobile) {
                                val newMRx = _state.value.mobileRxBytes + deltaRx
                                val newMTx = _state.value.mobileTxBytes + deltaTx
                                _state.value = _state.value.copy(
                                    mobileRxBytes = newMRx,
                                    mobileTxBytes = newMTx,
                                    isMobileNetwork = true
                                )
                                prefs.edit().putLong(KEY_MOBILE_RX, newMRx).putLong(KEY_MOBILE_TX, newMTx).apply()
                            } else {
                                val newWRx = _state.value.wifiRxBytes + deltaRx
                                val newWTx = _state.value.wifiTxBytes + deltaTx
                                _state.value = _state.value.copy(
                                    wifiRxBytes = newWRx,
                                    wifiTxBytes = newWTx,
                                    isMobileNetwork = false
                                )
                                prefs.edit().putLong(KEY_WIFI_RX, newWRx).putLong(KEY_WIFI_TX, newWTx).apply()
                            }
                            baselineRx = currentRx
                            baselineTx = currentTx
                        }
                    }

                    val sessionBytes = ((currentRx - sessionStartRx) + (currentTx - sessionStartTx)).coerceAtLeast(0L)
                    _state.value = _state.value.copy(sessionBytes = sessionBytes)
                }

                delay(5000)
            }
        }
    }

    fun toggleDataSaver(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DATA_SAVER, enabled).apply()
        _state.value = _state.value.copy(isDataSaverEnabled = enabled)
    }

    fun resetStats() {
        prefs.edit()
            .putLong(KEY_WIFI_RX, 0L)
            .putLong(KEY_WIFI_TX, 0L)
            .putLong(KEY_MOBILE_RX, 0L)
            .putLong(KEY_MOBILE_TX, 0L)
            .apply()

        val myUid = Process.myUid()
        baselineRx = TrafficStats.getUidRxBytes(myUid)
        baselineTx = TrafficStats.getUidTxBytes(myUid)
        sessionStartRx = baselineRx
        sessionStartTx = baselineTx

        _state.value = _state.value.copy(
            wifiRxBytes = 0L,
            wifiTxBytes = 0L,
            mobileRxBytes = 0L,
            mobileTxBytes = 0L,
            sessionBytes = 0L
        )
    }

    private fun checkIsMobile(): Boolean {
        return try {
            val net = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(net) ?: return false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } catch (_: Exception) {
            false
        }
    }
}
