package org.cemetery.app.feature.cemetery

import kotlin.math.*

/** Gesture arbitration is independent of Android views so pinch/drag/tap conflicts are testable. */
class CemeteryGestures(private val camera: CameraController, private val slop: Float, private val density: Float) {
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var span = 0f
    private var axis = 0 // 0 undecided, 1 horizontal, 2 vertical
    private var moved = false
    private var multiple = false

    fun down(x: Float, y: Float) {
        downX = x; downY = y; lastX = x; span = 0f
        axis = 0; moved = false; multiple = false
    }
    fun secondPointer(distance: Float) { multiple = true; moved = true; span = distance }
    fun move(x: Float, y: Float) {
        if (multiple) return
        val dx = x - downX
        val dy = y - downY
        if (hypot(dx, dy) > slop) {
            moved = true
            if (axis == 0) axis = if (abs(dx) > abs(dy)) 1 else 2
        }
        if (axis == 1) camera.panBy(-(x - lastX) / density * .025f * CameraController.DEFAULT_FOCAL / camera.focalLength)
        lastX = x
    }
    fun pinch(distance: Float) {
        if (span > slop && distance > slop) camera.zoomBy((distance / span).coerceIn(.8f, 1.25f))
        span = distance; multiple = true; moved = true
    }
    fun pointerUp() { multiple = true; moved = true }
    fun up(x: Float, y: Float): Boolean = !multiple && !moved && hypot(x - downX, y - downY) <= slop
    fun cancel() { moved = true }
}
