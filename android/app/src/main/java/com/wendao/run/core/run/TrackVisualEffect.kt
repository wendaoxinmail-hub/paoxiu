package com.wendao.run.core.run

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.wendao.run.ui.theme.PaoxiuColors

/**
 * 轨迹视觉特效：由灵根底色 + 装备叠加决定，用于跑步中与详情页地图。
 */
data class TrackVisualEffect(
    val label: String,
    val primaryColor: Int,
    val auraColor: Int,
    val accentColor: Int,
    val lineWidth: Int = 12,
    val auraWidth: Int = 22,
    val pulseGlow: Boolean = true,
) {
    companion object {
        val Default = TrackVisualEffect(
            label = "步罡初成",
            primaryColor = PaoxiuColors.KeepGreen.toArgb(),
            auraColor = Color(0x6624C789).toArgb(),
            accentColor = Color(0xFFC9A227).toArgb(),
        )
    }
}

object TrackEffectResolver {

    fun resolve(
        spiritRoot: String?,
        weaponId: String?,
        armorId: String?,
        accessoryId: String?,
    ): TrackVisualEffect {
        val root = rootStyle(spiritRoot)
        val weapon = weaponStyle(weaponId)
        val armor = armorStyle(armorId)
        val accessory = accessoryStyle(accessoryId)

        val primary = weapon?.primaryColor ?: root.primaryColor ?: TrackVisualEffect.Default.primaryColor
        val aura = accessory?.auraColor ?: root.auraColor ?: TrackVisualEffect.Default.auraColor
        val accent = root.accentColor ?: weapon?.primaryColor ?: TrackVisualEffect.Default.accentColor
        val lineWidth = 10 + (weapon?.lineBonus ?: 0) + (armor?.lineBonus ?: 0)
        val auraWidth = 20 + (accessory?.auraBonus ?: 0) + (armor?.auraBonus ?: 0)

        val parts = buildList {
            root.labelPart?.let { add(it) }
            weapon?.labelPart?.let { add(it) }
            armor?.labelPart?.let { add(it) }
            accessory?.labelPart?.let { add(it) }
        }

        return TrackVisualEffect(
            label = parts.joinToString(" · ").ifBlank { "步罡初成" },
            primaryColor = primary,
            auraColor = aura,
            accentColor = accent,
            lineWidth = lineWidth.coerceIn(10, 20),
            auraWidth = auraWidth.coerceIn(18, 36),
            pulseGlow = root.pulseGlow || (weapon?.pulseGlow == true),
        )
    }

    fun previewForEquipment(equipmentId: String, spiritRoot: String? = null): TrackVisualEffect {
        return when (equipmentId) {
            "wood_sword" -> resolve(spiritRoot, "wood_sword", null, null)
            "iron_boots" -> resolve(spiritRoot, null, "iron_boots", null)
            "spirit_pendant" -> resolve(spiritRoot, null, null, "spirit_pendant")
            "moon_robe" -> resolve(spiritRoot, null, "moon_robe", null)
            else -> resolve(spiritRoot, null, null, null)
        }
    }

    fun previewForSpiritRoot(spiritRoot: String): TrackVisualEffect =
        resolve(spiritRoot, null, null, null)

    private data class LayerStyle(
        val primaryColor: Int? = null,
        val auraColor: Int? = null,
        val accentColor: Int? = null,
        val lineBonus: Int = 0,
        val auraBonus: Int = 0,
        val labelPart: String? = null,
        val pulseGlow: Boolean = false,
    )

    private fun rootStyle(root: String?): LayerStyle = when (root) {
        "fire" -> LayerStyle(
            primaryColor = Color(0xFFE05555).toArgb(),
            auraColor = Color(0x66FF6633).toArgb(),
            accentColor = Color(0xFFFFAA44).toArgb(),
            labelPart = "火灵根焰纹",
            pulseGlow = true,
        )
        "water" -> LayerStyle(
            primaryColor = Color(0xFF24C789).toArgb(),
            auraColor = Color(0x6624C789).toArgb(),
            accentColor = Color(0xFF5EC4B6).toArgb(),
            labelPart = "水灵根流光",
        )
        "earth" -> LayerStyle(
            primaryColor = Color(0xFFC9A227).toArgb(),
            auraColor = Color(0x66C9A227).toArgb(),
            accentColor = Color(0xFF8B6914).toArgb(),
            labelPart = "土灵根金线",
        )
        "wind" -> LayerStyle(
            primaryColor = Color(0xFF5EC4B6).toArgb(),
            auraColor = Color(0x665EC4B6).toArgb(),
            accentColor = Color(0xFFB8F0E8).toArgb(),
            labelPart = "风灵根逸动",
            pulseGlow = true,
        )
        "thunder" -> LayerStyle(
            primaryColor = Color(0xFF7B68EE).toArgb(),
            auraColor = Color(0x667B68EE).toArgb(),
            accentColor = Color(0xFFE0D4FF).toArgb(),
            labelPart = "雷灵根闪纹",
            pulseGlow = true,
        )
        "chaos" -> LayerStyle(
            primaryColor = Color(0xFFD4AF37).toArgb(),
            auraColor = Color(0x6624C789).toArgb(),
            accentColor = Color(0xFFE05555).toArgb(),
            labelPart = "混沌灵根",
            pulseGlow = true,
        )
        else -> LayerStyle(
            primaryColor = PaoxiuColors.KeepGreen.toArgb(),
            auraColor = Color(0x6624C789).toArgb(),
            accentColor = Color(0xFFC9A227).toArgb(),
            labelPart = if (root.isNullOrBlank()) null else "灵根显化",
        )
    }

    private fun weaponStyle(id: String?): LayerStyle? = when (id) {
        "wood_sword" -> LayerStyle(
            primaryColor = Color(0xFF3D8B40).toArgb(),
            lineBonus = 2,
            labelPart = "木剑剑气",
        )
        else -> null
    }

    private fun armorStyle(id: String?): LayerStyle? = when (id) {
        "iron_boots" -> LayerStyle(
            lineBonus = 3,
            auraBonus = 2,
            labelPart = "铁履步罡",
        )
        "moon_robe" -> LayerStyle(
            auraColor = Color(0x55E8F0FF).toArgb(),
            auraBonus = 8,
            labelPart = "月华袍辉",
        )
        else -> null
    }

    private fun accessoryStyle(id: String?): LayerStyle? = when (id) {
        "spirit_pendant" -> LayerStyle(
            auraColor = Color(0x775EC4B6).toArgb(),
            auraBonus = 10,
            labelPart = "聚灵佩光晕",
        )
        else -> null
    }
}
