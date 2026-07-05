package com.wendao.run.core.run

import com.wendao.run.core.weather.WeatherSnapshot
import kotlin.math.abs
import kotlin.math.exp

/**
 * 气机指数：结合户外天气舒适度，越适宜修炼灵气越高。
 *
 * 配速仅作次要参考；无天气数据时回退速度估算。
 */
object QiIndexUtils {

    /** 户外跑步/修炼最适气温（℃） */
    private const val IDEAL_TEMP_C = 18.0

    /**
     * 由天气计算舒适度 → 气机指数（0..100）。
     */
    fun comfortFromWeather(
        tempC: Double,
        humidityPercent: Int,
        windKmh: Double,
        weatherCode: Int,
    ): Int {
        val tempScore = gaussianScore(tempC, IDEAL_TEMP_C, 7.0)
        val humidityScore = gaussianScore(humidityPercent.toDouble(), 55.0, 18.0)
        val windScore = when {
            windKmh <= 12.0 -> 100.0
            windKmh <= 20.0 -> 100.0 - (windKmh - 12.0) * 4.0
            else -> (100.0 - (windKmh - 20.0) * 6.0).coerceAtLeast(20.0)
        }
        val weatherScore = weatherCodeScore(weatherCode)
        val raw = tempScore * 0.38 + humidityScore * 0.22 + windScore * 0.18 + weatherScore * 0.22
        return raw.toInt().coerceIn(0, 100)
    }

    fun fromWeather(weather: WeatherSnapshot?): Int =
        weather?.comfortIndex ?: 0

    /** 兼容历史报表：无天气时用配速/速度粗估 */
    fun estimate(paceSecPerKm: Double?, currentSpeedKmh: Float): Int {
        if (paceSecPerKm != null && paceSecPerKm > 0) {
            val diff = abs(paceSecPerKm - 360.0)
            return (75 - diff / 12).toInt().coerceIn(35, 85)
        }
        if (currentSpeedKmh > 0.5f) {
            return (currentSpeedKmh * 10).toInt().coerceIn(35, 85)
        }
        return 0
    }

    /** 将指数映射为修仙 + 舒适度文案 */
    fun label(index: Int): String = when {
        index <= 0 -> "推演中…"
        index < 35 -> "灵气拮滞"
        index < 55 -> "灵气尚可"
        index < 75 -> "灵气平和"
        index < 90 -> "灵气充盈"
        else -> "天机气和"
    }

    fun comfortHint(weather: WeatherSnapshot?): String? {
        weather ?: return null
        return buildString {
            append("${weather.summary} ")
            append("${weather.tempC.toInt()}℃")
            append(" · 体感温度${weather.feelsLikeC.toInt()}℃")
            append(" · 湿度${weather.humidityPercent}%")
            if (weather.windKmh >= 8) append(" · 风${weather.windKmh.toInt()}km/h")
        }
    }

    private fun weatherCodeScore(code: Int): Double = when (code) {
        0 -> 100.0
        1, 2, 3 -> 88.0
        45, 48 -> 55.0
        51, 53, 55 -> 45.0
        61, 63, 65 -> 35.0
        71, 73, 75 -> 30.0
        80, 81, 82 -> 40.0
        95, 96, 99 -> 20.0
        else -> 65.0
    }

    /** 高斯型得分：越接近 ideal 越高，spread 控制容忍宽度 */
    private fun gaussianScore(value: Double, ideal: Double, spread: Double): Double {
        val d = (value - ideal) / spread
        return (100.0 * exp(-0.5 * d * d)).coerceIn(0.0, 100.0)
    }
}
