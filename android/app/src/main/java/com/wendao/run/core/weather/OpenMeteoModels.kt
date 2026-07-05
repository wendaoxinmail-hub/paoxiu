package com.wendao.run.core.weather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenMeteoResponse(
    val current: OpenMeteoCurrent? = null,
)

@Serializable
data class OpenMeteoCurrent(
    @SerialName("temperature_2m") val temperature2m: Double? = null,
    @SerialName("apparent_temperature") val apparentTemperature: Double? = null,
    @SerialName("relative_humidity_2m") val relativeHumidity2m: Int? = null,
    @SerialName("weather_code") val weatherCode: Int? = null,
    /** Open-Meteo 默认 km/h */
    @SerialName("wind_speed_10m") val windSpeed10m: Double? = null,
)
