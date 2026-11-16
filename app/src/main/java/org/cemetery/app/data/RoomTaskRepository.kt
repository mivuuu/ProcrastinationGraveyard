package org.cemetery.app.data
import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.*
import org.cemetery.app.domain.*
import org.cemetery.app.feature.notifications.ReminderScheduler
import java.util.UUID
@Singleton class RoomTaskRepository @Inject constructor(private val db: CemeteryDatabase, private val preferences: Preferences, private val scheduler: ReminderScheduler) : TaskRepository {
    private val dao = db.dao()
    override val snapshot = combine(dao.observeTasks(), dao.observeGraves(), dao.observeHistory()) { t, g, h -> Snapshot(t.map { it.domain() }, g.map { it.domain() }, h.map { it.domain() }) }.onStart { reconcile() }
    private suspend fun reconcileInside(now: Long) {
        val graves = dao.graves().toMutableList()
        val style = preferences.settings.first().style
        for (entity in dao.tasks()) {
            val task = entity.domain()
            if (LifecycleRules.missed(task, now) && graves.none { it.id == LifecycleRules.graveKey(task) }) {
                val ordinal = dao.nextOrdinal() ?: 0
                dao.placement(PlacementEntity(nextOrdinal = ordinal + 1))
                val p = CemeteryPlacement.plot(ordinal)
                val g = GraveEntity(LifecycleRules.graveKey(task), task.id, task.deadline, p.x, p.z, p.chunk, p.variant, now,
                    EpitaphPicker.choose(graves.filter { it.style == style }.map { it.epitaphId }), style, task.completedAt != null)
                dao.grave(g); graves.add(g)
            }
        }
    }
    override suspend fun reconcile() { db.withTransaction { reconcileInside(System.currentTimeMillis()) } }
    override suspend fun save(id: String?, title: String, description: String, deadline: Long) {
        require(LifecycleRules.validate(title))
        val taskId = id ?: UUID.randomUUID().toString()
        db.withTransaction {
            val now = System.currentTimeMillis()
            reconcileInside(now)
            val old = id?.let { dao.task(it) }
            if (old != null && old.deadline != deadline) dao.history(HistoryEntity(UUID.randomUUID().toString(), taskId, now, "rescheduled", old.deadline, deadline))
            dao.save(TaskEntity(taskId, title.trim(), description.trim(), old?.createdAt ?: now, old?.originalDeadline ?: deadline, deadline, old?.completedAt))
            reconcileInside(now)
        }
        scheduler.schedule(taskId, deadline)
    }
    override suspend fun complete(id: String) {
        db.withTransaction {
            val now = System.currentTimeMillis(); reconcileInside(now)
            dao.task(id)?.takeIf { it.completedAt == null }?.let { dao.save(it.copy(completedAt = now)); dao.resurrect(id)
                dao.history(HistoryEntity(UUID.randomUUID().toString(), id, now, "completed", it.deadline, it.deadline)) }
        }
        scheduler.cancel(id)
    }
    override suspend fun delete(id: String) {
        db.withTransaction { dao.deleteGraves(id); dao.deleteHistory(id); dao.deleteTask(id) }; scheduler.cancel(id)
    }
}
