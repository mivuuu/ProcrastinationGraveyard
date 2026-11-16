package org.cemetery.app.core

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable

/** Shared, restrained motion. Geometry/layout sizes remain stable during visual feedback. */
object CemeteryMotion {
    const val PRESS=120
    const val SELECTION=220
    const val NAVIGATION=280
    const val SCREEN=260
    const val PANEL=300
    const val CARD=240
    val easing=CubicBezierEasing(.2f,0f,0f,1f)
    @Composable fun <T> spec(milliseconds:Int):TweenSpec<T> =
        tween(if(CemeteryDesign.motion)milliseconds else 0,easing=easing)
}
