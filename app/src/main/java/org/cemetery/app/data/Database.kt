package org.cemetery.app.data
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.cemetery.app.domain.*
@Entity(tableName = "tasks")
data class TaskEntity(@PrimaryKey val id: String, val title: String, val description: String, val createdAt: Long,
    val originalDeadline: Long, val deadline: Long, val completedAt: Long?) {
    fun domain() = Task(id, title, description, createdAt, originalDeadline, deadline, completedAt)
}
@Entity(tableName = "graves", indices = [Index(value = ["taskId", "missedDeadline"], unique = true)])
data class GraveEntity(@PrimaryKey val id: String, val taskId: String, val missedDeadline: Long, val x: Float, val z: Float,
    val chunk: Int, val variant: Int, val createdAt: Long, val epitaphId: Int, val style: String, val resurrected: Boolean) {
    fun domain() = Grave(id, taskId, missedDeadline, x, z, chunk, variant, createdAt, epitaphId, style, resurrected)
}
@Entity(tableName = "history", indices = [Index("taskId")])
data class HistoryEntity(@PrimaryKey val id: String, val taskId: String, val at: Long, val type: String, val previousDeadline: Long, val newDeadline: Long) {
    fun domain() = History(id, taskId, at, type, previousDeadline, newDeadline)
}
@Entity(tableName = "placement")
data class PlacementEntity(@PrimaryKey val id: Int = 1, val nextOrdinal: Int = 0)
@Dao
interface CemeteryDao {
    @Query("SELECT nextOrdinal FROM placement WHERE id = 1") suspend fun nextOrdinal(): Int?
    @Upsert suspend fun placement(state: PlacementEntity)
    @Query("SELECT * FROM tasks ORDER BY deadline") fun observeTasks(): Flow<List<TaskEntity>>
    @Query("SELECT * FROM graves ORDER BY createdAt, id") fun observeGraves(): Flow<List<GraveEntity>>
    @Query("SELECT * FROM history ORDER BY at") fun observeHistory(): Flow<List<HistoryEntity>>
    @Query("SELECT * FROM tasks") suspend fun tasks(): List<TaskEntity>
    @Query("SELECT * FROM graves ORDER BY createdAt, id") suspend fun graves(): List<GraveEntity>
    @Query("SELECT * FROM tasks WHERE id = :id") suspend fun task(id: String): TaskEntity?
    @Upsert suspend fun save(task: TaskEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun grave(grave: GraveEntity)
    @Insert suspend fun history(event: HistoryEntity)
    @Query("UPDATE graves SET resurrected = 1 WHERE taskId = :id") suspend fun resurrect(id: String)
    @Query("DELETE FROM graves WHERE taskId = :id") suspend fun deleteGraves(id: String)
    @Query("DELETE FROM history WHERE taskId = :id") suspend fun deleteHistory(id: String)
    @Query("DELETE FROM tasks WHERE id = :id") suspend fun deleteTask(id: String)
}
@Database(entities = [TaskEntity::class, GraveEntity::class, HistoryEntity::class, PlacementEntity::class], version = 3, exportSchema = true)
abstract class CemeteryDatabase : RoomDatabase() { abstract fun dao(): CemeteryDao }
