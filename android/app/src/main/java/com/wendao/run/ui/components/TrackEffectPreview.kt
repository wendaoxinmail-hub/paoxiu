package com.wendao.run.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.wendao.run.core.run.TrackVisualEffect

/** 轨迹特效预览条（装备库 / 灵根说明用） */
@Composable
fun TrackEffectPreview(
    effect: TrackVisualEffect,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
) {
    val transition = rememberInfiniteTransition(label = "track_preview")
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        val w = size.width
        val h = size.height
        val midY = h * 0.55f
        val path = Path().apply {
            moveTo(w * 0.05f, midY)
            cubicTo(w * 0.25f, midY - h * 0.35f, w * 0.45f, midY + h * 0.25f, w * 0.65f, midY - h * 0.1f)
            cubicTo(w * 0.78f, midY - h * 0.28f, w * 0.88f, midY + h * 0.15f, w * 0.95f, midY)
        }
        val auraWidth = effect.auraWidth * pulse
        drawPath(
            path = path,
            color = Color(effect.auraColor),
            style = Stroke(width = auraWidth, cap = StrokeCap.Round),
        )
        drawPath(
            path = path,
            color = Color(effect.primaryColor),
            style = Stroke(width = effect.lineWidth.toFloat(), cap = StrokeCap.Round),
        )
        drawPath(
            path = path,
            color = Color(effect.accentColor).copy(alpha = 0.65f),
            style = Stroke(width = (effect.lineWidth * 0.35f), cap = StrokeCap.Round),
        )
        // 终点光点
        drawCircle(
            color = Color(effect.accentColor),
            radius = 6f * pulse,
            center = Offset(w * 0.95f, midY),
        )
    }
    if (showLabel) {
        Text(
            text = "轨迹特效 · ${effect.label}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
