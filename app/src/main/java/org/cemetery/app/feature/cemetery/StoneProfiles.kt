package org.cemetery.app.feature.cemetery

import kotlin.math.*

/** Closed, convex silhouettes, extruded with front/back bevels by the mesh factory. */
internal object StoneProfiles {
    fun outline(variant: Int): List<Pair<Float, Float>> = when (variant.mod(4)) {
        1 -> roundedRectangle(.62f, 1.72f, .18f)
        2 -> arch(.72f, .94f, .48f)
        3 -> listOf(-.65f to 0f, .65f to 0f) +
            (0..20).map { val a=it*PI.toFloat()/20; cos(a)*.55f to 1.10f+sin(a)*.59f }
        else -> arch(.60f, 1.10f, .60f)
    }
    private fun arch(width: Float, shoulder: Float, rise: Float) =
        listOf(-width to 0f, width to 0f) + (0..24).map { val a=it*PI.toFloat()/24; cos(a)*width to shoulder+sin(a)*rise }
    private fun roundedRectangle(w: Float, h: Float, r: Float) = buildList {
        val corners=listOf(Triple(w-r,r,-90f),Triple(w-r,h-r,0f),Triple(-w+r,h-r,90f),Triple(-w+r,r,180f))
        corners.forEach { (x,y,start) -> repeat(7) { val a=(start+it*15)*PI.toFloat()/180;add(x+cos(a)*r to y+sin(a)*r) } }
    }
}
