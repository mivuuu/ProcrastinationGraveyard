package org.cemetery.app
import android.Manifest
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.cemetery.app.feature.notifications.WorkerDependencies
import org.junit.Test
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class DeniedReminderTest {
    @Test fun deniedPermissionLeavesTasksUsable()=runBlocking<Unit> {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue(Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
        val deps=EntryPointAccessors.fromApplication(context,WorkerDependencies::class.java)
        val prefs=deps.preferences();val original=prefs.settings.first()
        val id="denied-reminder-test"
        try {
            prefs.reminders(true);prefs.lead(60)
            deps.repository().save(id,"Denied reminder test","",System.currentTimeMillis()+30*60_000)
            val until=System.currentTimeMillis()+15_000
            var finished=false
            do {
                android.os.SystemClock.sleep(100)
                finished=WorkManager.getInstance(context).getWorkInfosForUniqueWork("reminder:$id").get().any{it.state.isFinished}
            }while(!finished && System.currentTimeMillis()<until)
            assertTrue(finished)
            assertTrue(context.getSystemService(NotificationManager::class.java).activeNotifications.none{it.id==id.hashCode()})
            deps.repository().complete(id)
            assertNotNull(deps.repository().snapshot.first().tasks.find{it.id==id}?.completedAt)
        }finally {deps.repository().delete(id);prefs.reminders(original.reminders);prefs.lead(original.leadMinutes)}
    }
}
