package org.cemetery.app.core
import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.cemetery.app.data.*
import org.cemetery.app.domain.TaskRepository
@Module @InstallIn(SingletonComponent::class)
object Injection {
    @Provides @Singleton fun database(@ApplicationContext context: Context): CemeteryDatabase = Room.databaseBuilder(context, CemeteryDatabase::class.java, "cemetery.db").addMigrations(org.cemetery.app.data.Migrations.FROM_1_TO_2, org.cemetery.app.data.Migrations.FROM_2_TO_3).build()
    @Provides fun repository(impl: RoomTaskRepository): TaskRepository = impl
}
