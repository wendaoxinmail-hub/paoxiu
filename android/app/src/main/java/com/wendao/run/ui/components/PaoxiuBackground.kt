package com.wendao.run.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.wendao.run.ui.theme.PaoxiuColors

/**
 * Keep 式浅灰画布 + 顶部绢帛暖色晕染，保留一丝国风意境。
 */
@Composable
fun PaoxiuBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PaoxiuColors.InkWash.copy(alpha = 0.55f),
                        PaoxiuColors.KeepCanvas,
                        PaoxiuColors.KeepCanvas,
                    ),
                ),
            ),
        content = content,
    )
}
