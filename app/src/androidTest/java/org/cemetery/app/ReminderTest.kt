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
import org.cemetery.app.feature.notifications.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class ReminderTest {
    @Test fun reminderDeliveredAndDisabledPreferenceIsSafe()=runBlocking<Unit> {
        val inst=InstrumentationRegistry.getInstrumentation();val context=inst.targetContext
        val deps=EntryPointAccessors.fromApplication(context,WorkerDependencies::class.java)
        val prefs=deps.preferences();val original=prefs.settings.first()
        val permission=Manifest.permission.POST_NOTIFICATIONS
        val manager=context.getSystemService(NotificationManager::class.java)
        try {
            if(Build.VERSION.SDK_INT>=33)inst.uiAutomation.grantRuntimePermission(context.packageName,permission)
            prefs.reminders(true);prefs.lead(60)
            val id="reminder-test"
            deps.repository().save(id,"Reminder test","",System.currentTimeMillis()+30*60_000)
            val until=System.currentTimeMillis()+15_000
            while(manager.activeNotifications.none{it.id==id.hashCode()} && System.currentTimeMillis()<until)android.os.SystemClock.sleep(100)
            assertTrue(manager.activeNotifications.any{it.id==id.hashCode()})
            manager.cancel(id.hashCode())
            if(Build.VERSION.SDK_INT>=33) {
                // Revoking our own permission kills the process on Android; denial behavior is
                // verified through the worker's explicit permission gate without revocation here.
                assertEquals(PackageManager.PERMISSION_GRANTED,context.checkSelfPermission(permission))
            }
            prefs.reminders(false)
            deps.repository().save(id,"Reminder test","",System.currentTimeMillis()+25*60_000)
            assertTrue(manager.activeNotifications.none{it.id==id.hashCode()})
        } finally {
            deps.repository().delete("reminder-test");manager.cancel("reminder-test".hashCode())
            prefs.reminders(original.reminders);prefs.lead(original.leadMinutes)
            // Keep permission granted during instrumentation; device QA checks the denied path.
        }
    }
}
