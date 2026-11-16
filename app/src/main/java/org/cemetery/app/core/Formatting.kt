package org.cemetery.app.core
import android.content.Context
import org.cemetery.app.R
import org.cemetery.app.domain.TaskStatus
import java.time.*
import java.time.format.*
fun dateTime(context: Context, stamp: Long): String = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(context.resources.configuration.locales[0]).withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(stamp))
fun statusResource(status: TaskStatus) = when(status) { TaskStatus.ACTIVE -> R.string.active; TaskStatus.APPROACHING -> R.string.approaching; TaskStatus.OVERDUE -> R.string.overdue; TaskStatus.COMPLETED -> R.string.completed; TaskStatus.RESURRECTED -> R.string.resurrected }
fun duration(context: Context, millis: Long): String {
    val hours = (millis.coerceAtLeast(0)/3_600_000).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    return when { hours >= 24 -> context.resources.getQuantityString(R.plurals.duration_days,hours/24,hours/24); hours > 0 -> context.resources.getQuantityString(R.plurals.duration_hours,hours,hours); else -> context.getString(R.string.less_hour) }
}
