package com.wendao.run.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 统一页面边距与纵向间距，避免各 Tab 长短不一 */
object PaoxiuLayout {
    const val ScreenHorizontalDp = 16
    const val ScreenVerticalDp = 12
    const val SectionSpacingDp = 12
    const val MapHeightDp = 260
}

@Composable
fun PaoxiuScreenContent(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val columnModifier = modifier
        .fillMaxSize()
        .padding(
            horizontal = PaoxiuLayout.ScreenHorizontalDp.dp,
            vertical = PaoxiuLayout.ScreenVerticalDp.dp,
        )
        .let { base ->
            if (scrollable) base.verticalScroll(rememberScrollState()) else base
        }
    Column(modifier = columnModifier, content = content)
}
