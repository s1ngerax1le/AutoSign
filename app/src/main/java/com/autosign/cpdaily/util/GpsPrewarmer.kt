package com.autosign.cpdaily.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import com.autosign.cpdaily.service.SignAccessibilityService

/**
 * GPS 硬件高精度预热器
 * 在抢榜提前唤醒阶段 (如 06:29:40 ~ 06:30:08) 主动请求高精度定位，
 * 促使手机 GNSS 芯片与网络定位引擎进入热启动锁定状态，
 * 确保今日校园 WebView 请求经纬度时可在 <200ms 内瞬间秒出高精度坐标。
 */
object GpsPrewarmer {
    private const val TAG = "GpsPrewarmer"

    private var locationManager: LocationManager? = null
    private var isPrewarming = false
    private var lastLocation: Location? = null

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            lastLocation = location
            Log.d(TAG, "📍 GPS 预热采样就绪: provider=${location.provider}, accuracy=${location.accuracy}m")
            SignAccessibilityService.log("📍 [GPS保鲜中] 卫星/网络高精度定位已就绪 (精度 ${location.accuracy}m)")
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {
            Log.d(TAG, "Provider enabled: $provider")
        }
        override fun onProviderDisabled(provider: String) {
            Log.d(TAG, "Provider disabled: $provider")
        }
    }

    @SuppressLint("MissingPermission")
    fun startPrewarm(context: Context) {
        if (isPrewarming) return
        isPrewarming = true

        try {
            // 通过 Root 保障位置服务处于开启状态
            RootUtil.execCommand("cmd location set-location-enabled true", context)

            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            locationManager = lm

            if (lm != null) {
                // 1. 同时监听 GPS 与 NETWORK 提供商
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lm.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        500L,
                        0f,
                        locationListener,
                        Looper.getMainLooper()
                    )
                    Log.d(TAG, "已挂载 GPS_PROVIDER 预热监听")
                }

                if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lm.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        500L,
                        0f,
                        locationListener,
                        Looper.getMainLooper()
                    )
                    Log.d(TAG, "已挂载 NETWORK_PROVIDER 预热监听")
                }

                SignAccessibilityService.log("🛰️ [硬件预热] 启动高精度 GPS 芯片锁定，保持经纬度星历热态缓存...")
            }
        } catch (e: Exception) {
            Log.e(TAG, "启动 GPS 预热失败: ${e.message}")
            SignAccessibilityService.log("⚠️ GPS 预热警告: ${e.message}")
        }
    }

    fun stopPrewarm() {
        if (!isPrewarming) return
        try {
            locationManager?.removeUpdates(locationListener)
            Log.d(TAG, "已停止 GPS 预热")
        } catch (e: Exception) {
            Log.e(TAG, "停止 GPS 预热失败: ${e.message}")
        } finally {
            isPrewarming = false
            locationManager = null
        }
    }

    fun getLastKnownAccuracy(): Float? = lastLocation?.accuracy
}
