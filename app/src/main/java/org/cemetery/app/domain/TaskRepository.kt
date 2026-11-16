package org.cemetery.app.domain
import kotlinx.coroutines.flow.Flow
interface TaskRepository {
    val snapshot: Flow<Snapshot>
    suspend fun save(id: String?, title: String, description: String, deadline: Long)
    suspend fun complete(id: String)
    suspend fun delete(id: String)
    suspend fun reconcile()
}
