package org.cemetery.app.feature.notifications
import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import kotlinx.coroutines.flow.first
import org.cemetery.app.*
import org.cemetery.app.R
import org.cemetery.app.data.*
import org.cemetery.app.domain.TaskRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
@EntryPoint @InstallIn(SingletonComponent::class)
interface WorkerDependencies { fun repository(): TaskRepository; fun preferences(): Preferences; fun database(): CemeteryDatabase }
@Singleton class ReminderScheduler @Inject constructor(@ApplicationContext private val context: Context, private val preferences: Preferences) {
    suspend fun schedule(id: String, deadline: Long) {
        val settings = preferences.settings.first()
        cancel(id)
        if (!settings.reminders || deadline <= System.currentTimeMillis()) return
        val trigger = deadline - settings.leadMinutes * 60_000L
        val request = OneTimeWorkRequestBuilder<ReminderWorker>().setInputData(workDataOf("task" to id, "deadline" to deadline))
            .setInitialDelay((trigger - System.currentTimeMillis()).coerceAtLeast(0), TimeUnit.MILLISECONDS).build()
        WorkManager.getInstance(context).enqueueUniqueWork("reminder:$id", ExistingWorkPolicy.REPLACE, request)
    }
    fun cancel(id: String) { WorkManager.getInstance(context).cancelUniqueWork("reminder:$id") }
    fun startReconciliation() { WorkManager.getInstance(context).enqueueUniquePeriodicWork("reconcile", ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<ReconcileWorker>(15, TimeUnit.MINUTES).build()) }
}
class ReconcileWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try { EntryPointAccessors.fromApplication(applicationContext, WorkerDependencies::class.java).repository().reconcile(); Result.success() } catch (_: Exception) { Result.retry() }
}
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, WorkerDependencies::class.java)
        val settings = deps.preferences().settings.first()
        if (!settings.reminders) return Result.success()
        val task = deps.database().dao().task(inputData.getString("task") ?: return Result.success()) ?: return Result.success()
        if (task.completedAt != null || task.deadline != inputData.getLong("deadline", 0) || task.deadline < System.currentTimeMillis()) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return Result.success()
        val context = org.cemetery.app.core.localizedContext(applicationContext, settings.language)
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("deadlines", context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT))
        val intent = PendingIntent.getActivity(applicationContext, 0, Intent(applicationContext, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        manager.notify(task.id.hashCode(), NotificationCompat.Builder(context, "deadlines").setSmallIcon(R.drawable.ic_cemetery).setContentTitle(context.getString(R.string.reminder_title)).setContentText(context.getString(R.string.reminder_body, task.title)).setContentIntent(intent).setAutoCancel(true).build())
        return Result.success()
    }
}
