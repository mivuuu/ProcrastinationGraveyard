package org.cemetery.app
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.cemetery.app.data.*
import org.cemetery.app.feature.notifications.ReminderScheduler
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class RepositoryTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db:CemeteryDatabase
    private lateinit var repo:RoomTaskRepository
    @Before fun setUp(){db=Room.inMemoryDatabaseBuilder(context,CemeteryDatabase::class.java).build();val prefs=Preferences(context);repo=RoomTaskRepository(db,prefs,ReminderScheduler(context,prefs))}
    @After fun tearDown(){db.close()}
    @Test fun createEditCompleteDelete()=runBlocking {
        repo.save(null,"First promise","Details",System.currentTimeMillis()+86_400_000)
        val original=repo.snapshot.first().tasks.single()
        repo.save(original.id,"Renamed","Edited",original.deadline)
        assertEquals("Renamed",repo.snapshot.first().tasks.single().title)
        repo.complete(original.id)
        assertNotNull(repo.snapshot.first().tasks.single().completedAt)
        assertTrue(repo.snapshot.first().graves.isEmpty())
        repo.delete(original.id);assertTrue(repo.snapshot.first().tasks.isEmpty())
    }
    @Test fun overdueDedupRescheduleResurrect()=runBlocking {
        repo.save(null,"A late promise","",System.currentTimeMillis()-86_400_000)
        val first=repo.snapshot.first();val task=first.tasks.single();val grave=first.graves.single()
        repeat(3){repo.reconcile()};assertEquals(1,repo.snapshot.first().graves.size)
        repo.save(task.id,task.title,"",System.currentTimeMillis()+86_400_000)
        val rescheduled=repo.snapshot.first();assertEquals(grave,rescheduled.graves.single());assertEquals(task.originalDeadline,rescheduled.tasks.single().originalDeadline);assertEquals(1,rescheduled.history.size)
        repo.complete(task.id);assertTrue(repo.snapshot.first().graves.single().resurrected)
    }
    @Test fun repeatedMissCreatesOneGravePerDeadline()=runBlocking {
        repo.save(null,"Again","",System.currentTimeMillis()-100_000)
        val task=repo.snapshot.first().tasks.single()
        repo.save(task.id,task.title,"",System.currentTimeMillis()-50_000)
        repeat(3){repo.reconcile()};assertEquals(2,repo.snapshot.first().graves.size)
    }
    @Test fun deletionNeverReusesHistoricalPlot()=runBlocking {
        repo.save(null,"First","",System.currentTimeMillis()-100_000)
        val first=repo.snapshot.first();repo.delete(first.tasks.single().id)
        repo.save(null,"Second","",System.currentTimeMillis()-100_000)
        assertNotEquals(first.graves.single().x to first.graves.single().z,repo.snapshot.first().graves.single().let{it.x to it.z})
    }
    @Test fun persistedDatabaseSurvivesReopen()=runBlocking<Unit> {
        val name="repository-restart-test.db";context.deleteDatabase(name)
        val prefs=Preferences(context);val scheduler=ReminderScheduler(context,prefs)
        var disk=Room.databaseBuilder(context,CemeteryDatabase::class.java,name).build()
        val diskRepo=RoomTaskRepository(disk,prefs,scheduler);diskRepo.save(null,"Persistent","",System.currentTimeMillis()-1000)
        val original=diskRepo.snapshot.first();disk.close()
        disk=Room.databaseBuilder(context,CemeteryDatabase::class.java,name).build()
        val restored=RoomTaskRepository(disk,prefs,scheduler).snapshot.first()
        assertEquals(original.tasks,restored.tasks);assertEquals(original.graves,restored.graves)
        disk.close();context.deleteDatabase(name)
    }
}
