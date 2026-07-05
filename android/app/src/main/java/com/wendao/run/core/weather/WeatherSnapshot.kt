package com.wendao.run.core.weather

/** Open-Meteo 当前天气快照，用于修炼舒适度 / 气机指数。 */
data class WeatherSnapshot(
    val tempC: Double,
    val feelsLikeC: Double,
    val humidityPercent: Int,
    /** 10m 风速，km/h */
    val windKmh: Double,
    val weatherCode: Int,
    /** 0..100，越高越适宜户外修炼 */
    val comfortIndex: Int,
    val summary: String,
)
