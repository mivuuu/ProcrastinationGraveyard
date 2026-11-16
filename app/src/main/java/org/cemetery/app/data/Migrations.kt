package org.cemetery.app.data
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
object Migrations {
    val FROM_1_TO_2 = object : Migration(1,2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `placement` (`id` INTEGER NOT NULL, `nextOrdinal` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            // Start beyond every existing populated chunk, including holes left by deletions.
            db.execSQL("INSERT INTO placement(id,nextOrdinal) SELECT 1,COALESCE((MAX(chunk)+1)*16,0) FROM graves")
        }
    }
    val FROM_2_TO_3 = object : Migration(2,3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // One deterministic layout conversion. Only spatial columns change; IDs,
            // associations, epitaphs, timestamps, resurrection and task history survive.
            var ordinal = 0
            db.query("SELECT id FROM graves ORDER BY createdAt,id").use { rows ->
                while (rows.moveToNext()) {
                    val plot = org.cemetery.app.domain.CemeteryPlacement.plot(ordinal++)
                    db.execSQL("UPDATE graves SET x=?,z=?,chunk=? WHERE id=?", arrayOf(plot.x,plot.z,plot.chunk,rows.getString(0)))
                }
            }
            db.execSQL("INSERT OR REPLACE INTO placement(id,nextOrdinal) VALUES(1,?)", arrayOf(ordinal))
        }
    }
}
