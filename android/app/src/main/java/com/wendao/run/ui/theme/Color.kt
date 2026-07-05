package com.wendao.run.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 跑修色彩体系：Keep 浅色运动底 + 国风赤金点缀。
 *
 * Keep 绿为主行动色，绢帛暖白与淡墨灰做层次，赤金仅用于境界/法纹等修仙元素。
 */
object PaoxiuColors {

    // —— Keep 基底 ——
    /** 页面画布灰白 */
    val KeepCanvas = Color(0xFFF5F6F8)

    /** 卡片/表面白 */
    val KeepSurface = Color(0xFFFFFFFF)

    /** Keep 主色绿（开始修炼、选中 Tab、主按钮） */
    val KeepGreen = Color(0xFF24C789)

    /** Keep 深绿（按下态、渐变底） */
    val KeepGreenDark = Color(0xFF18A66F)

    /** 主文字 */
    val KeepTextPrimary = Color(0xFF1F2328)

    /** 辅助文字 */
    val KeepTextSecondary = Color(0xFF8E9399)

    /** 分隔线 */
    val KeepDivider = Color(0xFFE8EAED)

    /** 区块浅灰底 */
    val KeepSectionBg = Color(0xFFF0F2F5)

    // —— 国风点缀 ——
    /** 绢帛暖白，国风区块底色 */
    val InkWash = Color(0xFFF7F3EA)

    /** 赤金法纹，境界/边框强调 */
    val ImmortalGold = Color(0xFFC9A227)

    /** 灵气流光，与 Keep 绿统一 */
    val SpiritJade = KeepGreen

    /** 心魔赤，警示/收功 */
    val HeartDemon = Color(0xFFE05555)

    /** 突破灵光，浅绿高亮底 */
    val BreakthroughGlow = Color(0xFFE8F8F0)

    /** 导航栏白底 */
    val NavBar = Color(0xFFFFFFFF)

    // —— 水墨开屏（与 App 图标统一） ——
    /** 宣纸底色 */
    val Parchment = Color(0xFFEBE4D5)

    /** 宣纸深晕 */
    val ParchmentDeep = Color(0xFFE2D9CC)

    /** 淡墨 */
    val InkBlack = Color(0xFF1A1A1A)

    /** 远山淡墨 */
    val InkMountainFar = Color(0x338A8580)

    val InkMountainMid = Color(0x558A8580)

    val InkMountainNear = Color(0x778A8580)

    /** 流云 */
    val InkMist = Color(0xFFF5F0E8)

    /** 朱印红 */
    val SealRed = Color(0xFFB83232)

    /** 江水淡墨 */
    val InkWater = Color(0xFF7BA3A8)

    val InkWaterDeep = Color(0xFF5E868C)

    val InkWaterRipple = Color(0x558BA5AA)

    // —— 兼容旧名（映射到浅色语义） ——
    val InkNight = KeepCanvas
    val MistBlue = KeepSurface
    val GrottoTeal = KeepSectionBg
    val SilkWhite = KeepTextPrimary
    val InkMuted = KeepTextSecondary
}
