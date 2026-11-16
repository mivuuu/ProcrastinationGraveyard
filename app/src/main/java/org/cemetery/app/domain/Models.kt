package org.cemetery.app.domain

import kotlin.math.*
import kotlin.random.Random

enum class TaskStatus { ACTIVE, APPROACHING, OVERDUE, RESURRECTED, COMPLETED }
data class Task(val id: String, val title: String, val description: String, val createdAt: Long,
    val originalDeadline: Long, val deadline: Long, val completedAt: Long? = null) {
    fun status(now: Long): TaskStatus = when {
        completedAt != null -> if (completedAt > deadline) TaskStatus.RESURRECTED else TaskStatus.COMPLETED
        now > deadline -> TaskStatus.OVERDUE
        now >= deadline - minOf(86_400_000L, ((deadline - createdAt).coerceAtLeast(0) / 4)) -> TaskStatus.APPROACHING
        else -> TaskStatus.ACTIVE
    }
    fun health(now: Long): Float = ((deadline - now).toDouble() / (deadline - createdAt).coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
}
data class Grave(val id: String, val taskId: String, val missedDeadline: Long, val x: Float, val z: Float,
    val chunk: Int, val variant: Int, val createdAt: Long, val epitaphId: Int, val style: String, val resurrected: Boolean = false)
data class History(val id: String, val taskId: String, val at: Long, val type: String, val previousDeadline: Long, val newDeadline: Long)
data class Snapshot(val tasks: List<Task> = emptyList(), val graves: List<Grave> = emptyList(), val history: List<History> = emptyList())
data class Plot(val x: Float, val z: Float, val chunk: Int, val variant: Int)

object CemeteryPlacement {
    const val ROWS = 4
    const val COLUMN_SPACING = 3.6f
    const val ROW_SPACING = 3.8f
    const val FRONT_Z = 2.2f
    const val SECTION_WIDTH = COLUMN_SPACING * 4
    const val SPACING = 2.8f

    fun plot(index: Int): Plot {
        require(index >= 0)
        val row = index % ROWS
        val columnIndex = index / ROWS
        // Fill all four depth bands first, alternating new columns right and left.
        val column = when {
            columnIndex == 0 -> 0
            columnIndex % 2 == 1 -> (columnIndex + 1) / 2
            else -> -columnIndex / 2
        }
        val random = Random(0xD104A xor index)
        val x = column * COLUMN_SPACING + (row % 2 - .5f) * .24f + random.nextFloat() * .5f - .25f
        val z = FRONT_Z - row * ROW_SPACING + random.nextFloat() * .5f - .25f
        return Plot(x, z, sectionAt(x), random.nextInt(4))
    }

    fun sectionAt(x: Float): Int = floor((x + SECTION_WIDTH / 2) / SECTION_WIDTH).toInt()
    fun nearby(cameraX: Float, radius: Int = 2): IntRange {
        val center = sectionAt(cameraX)
        return center - radius..center + radius
    }
}
object EpitaphPicker {
    const val COUNT = 24
    fun choose(recent: List<Int>, random: Random = Random.Default): Int {
        val available = (0 until COUNT).filterNot { it in recent.takeLast(12) }
        return available.random(random)
    }
}
object LifecycleRules {
    fun missed(task: Task, now: Long): Boolean = task.deadline < (task.completedAt ?: now)
    fun graveKey(task: Task) = "${task.id}:${task.deadline}"
    fun validate(title: String) = title.isNotBlank() && title.length <= 200
}
