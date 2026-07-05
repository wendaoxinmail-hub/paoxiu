package com.wendao.run.ui.splash

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wendao.run.ui.theme.PaoxiuColors

/** 朱印题签，与图标右下角「跑修」印章呼应 */
@Composable
fun InkSealLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .border(
                width = 2.dp,
                color = PaoxiuColors.SealRed,
                shape = RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = PaoxiuColors.SealRed,
            ),
        )
    }
}
