package com.wendao.run.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class TrackPointDto(
    val lat: Double,
    val lng: Double,
    val recordedAt: String,
    val accuracyM: Float,
    val speedKmh: Float,
)

@Serializable
data class FinishRunRequest(
    val clientRunId: String,
    val startedAt: String,
    val finishedAt: String,
    val durationSeconds: Int,
    val distanceMeters: Double,
    val avgPaceSecPerKm: Double,
    val maxSpeedKmh: Double,
    val runType: String = "NORMAL",
    val points: List<TrackPointDto>,
)

@Serializable
data class FinishRunResponse(
    val serverRunId: Long? = null,
    val clientRunId: String,
    val status: String,
    val rejectReason: String? = null,
    val acceptedDistanceMeters: Double,
    val durationSeconds: Int,
    val avgPaceSecPerKm: Double,
    val message: String,
    val rewards: RunRewardDto? = null,
)

@Serializable
data class RunSummaryDto(
    val id: Long,
    val clientRunId: String,
    val startedAt: String,
    val finishedAt: String,
    val durationSeconds: Int,
    val distanceMeters: Double,
    val avgPaceSecPerKm: Double,
    val status: String,
    val runType: String,
)
