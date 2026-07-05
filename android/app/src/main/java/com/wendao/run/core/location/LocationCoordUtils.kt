package com.wendao.run.core.location

import android.location.LocationManager
import com.baidu.mapapi.model.LatLng
import com.baidu.mapapi.utils.CoordinateConverter
import com.wendao.run.core.run.RunGeoUtils

/** 系统 GPS 为 WGS84，百度地图/定位 SDK 使用 GCJ02。 */
internal fun wgs84ToGcj02(lat: Double, lng: Double): Pair<Double, Double> {
    val converter = CoordinateConverter()
        .from(CoordinateConverter.CoordType.GPS)
    converter.coord(LatLng(lat, lng))
    val converted = converter.convert() ?: return lat to lng
    return converted.latitude to converted.longitude
}

/**
 * 跑步坐标统一转 GCJ02（百度地图）。
 *
 * 国内部分机型 [LocationManager.GPS_PROVIDER] 已是 GCJ02，再 WGS84 转换会偏 ~300–700m。
 * 有上一锚点时，比较「原坐标 / 转换坐标」哪条更连贯，选更合理的一条。
 */
internal fun resolveRunCoordinate(
    provider: String,
    lat: Double,
    lng: Double,
    prevLat: Double?,
    prevLng: Double?,
): Pair<Double, Double> {
    return when (provider) {
        LocationManager.GPS_PROVIDER,
        LocationManager.PASSIVE_PROVIDER -> {
            if (prevLat == null || prevLng == null) {
                wgs84ToGcj02(lat, lng)
            } else {
                pickContinuityCoordinate(lat, lng, prevLat, prevLng)
            }
        }
        else -> lat to lng
    }
}

/** @deprecated 使用 [resolveRunCoordinate] */
internal fun androidLocationToGcj02(provider: String, lat: Double, lng: Double): Pair<Double, Double> =
    resolveRunCoordinate(provider, lat, lng, null, null)

private fun pickContinuityCoordinate(
    lat: Double,
    lng: Double,
    prevLat: Double,
    prevLng: Double,
): Pair<Double, Double> {
    val converted = wgs84ToGcj02(lat, lng)
    val rawJump = RunGeoUtils.haversineMeters(prevLat, prevLng, lat, lng)
    val gcjJump = RunGeoUtils.haversineMeters(prevLat, prevLng, converted.first, converted.second)
    return if (rawJump < gcjJump * 0.75 && rawJump < 120.0) {
        lat to lng
    } else {
        converted
    }
}
