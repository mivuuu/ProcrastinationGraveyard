package org.cemetery.app.core
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.cemetery.app.data.*
import org.cemetery.app.domain.*
import org.cemetery.app.feature.notifications.ReminderScheduler
import javax.inject.Inject
@HiltViewModel class AppViewModel @Inject constructor(private val repository: TaskRepository, private val preferences: Preferences, private val scheduler: ReminderScheduler) : ViewModel() {
    val snapshot = repository.snapshot.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Snapshot())
    val settings: StateFlow<Settings?> = preferences.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val now = MutableStateFlow(System.currentTimeMillis())
    val errors = MutableStateFlow(false)
    init {
        scheduler.startReconciliation()
        viewModelScope.launch { settings.filterNotNull().distinctUntilChanged().collect { s -> repository.snapshot.first().tasks.filter { it.completedAt == null }.forEach { scheduler.schedule(it.id, it.deadline) } } }
    }
    private fun act(block: suspend () -> Unit) { viewModelScope.launch { try { block(); now.value = System.currentTimeMillis() } catch (e: CancellationException) { throw e } catch (_: Exception) { errors.value = true } } }
    fun refresh() = act { repository.reconcile() }
    fun save(id: String?, title: String, description: String, deadline: Long) = act { repository.save(id, title, description, deadline) }
    fun complete(id: String) = act { repository.complete(id) }
    fun delete(id: String) = act { repository.delete(id) }
    fun text(key: String, value: String) = act { preferences.text(key, value) }
    fun reminders(value: Boolean) = act { preferences.reminders(value) }
    fun lead(value: Int) = act { preferences.lead(value) }
}
