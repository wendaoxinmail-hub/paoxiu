package com.wendao.run.core.weather

import com.wendao.run.core.run.QiIndexUtils
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class WeatherRepository @Inject constructor() {

    private val client = OkHttpClient.Builder().build()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchCurrent(lat: Double, lng: Double): WeatherSnapshot? = withContext(Dispatchers.IO) {
        if (!lat.isFinite() || !lng.isFinite()) return@withContext null
        val url =
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat&longitude=$lng" +
                "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m" +
                "&timezone=auto"
        runCatching {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val parsed = json.decodeFromString<OpenMeteoResponse>(body)
                val current = parsed.current ?: return@withContext null
                val temp = current.temperature2m ?: return@withContext null
                val feels = current.apparentTemperature ?: temp
                val humidity = current.relativeHumidity2m ?: 50
                val wind = current.windSpeed10m ?: 0.0
                val code = current.weatherCode ?: 0
                val comfort = QiIndexUtils.comfortFromWeather(
                    tempC = temp,
                    humidityPercent = humidity,
                    windKmh = wind,
                    weatherCode = code,
                )
                WeatherSnapshot(
                    tempC = temp,
                    feelsLikeC = feels,
                    humidityPercent = humidity,
                    windKmh = wind,
                    weatherCode = code,
                    comfortIndex = comfort,
                    summary = weatherCodeLabel(code),
                )
            }
        }.getOrNull()
    }

    private fun weatherCodeLabel(code: Int): String = when (code) {
        0 -> "晴"
        1, 2, 3 -> "多云"
        45, 48 -> "雾"
        51, 53, 55 -> "毛毛雨"
        61, 63, 65 -> "雨"
        71, 73, 75 -> "雪"
        80, 81, 82 -> "阵雨"
        95, 96, 99 -> "雷暴"
        else -> "阴晴未定"
    }
}
