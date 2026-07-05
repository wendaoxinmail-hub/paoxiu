package com.wendao.run.core.location

/**
 * 模拟器灵根试炼用路线（约 1.03km，与 demo-api.sh 一致）。
 */
object SpiritRootDemoRoute {
    data class Point(val lat: Double, val lng: Double)

    val points: List<Point> = listOf(
        Point(39.90420, 116.40740),
        Point(39.90550, 116.40780),
        Point(39.90680, 116.40820),
        Point(39.90810, 116.40860),
        Point(39.90940, 116.40900),
        Point(39.91070, 116.40940),
        Point(39.91200, 116.40980),
        Point(39.91330, 116.41020),
    )
}
