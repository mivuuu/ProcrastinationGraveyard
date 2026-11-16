package org.cemetery.app
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.cemetery.app.data.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule val helper=MigrationTestHelper(InstrumentationRegistry.getInstrumentation(),CemeteryDatabase::class.java)
    @Test fun migrationKeepsRecordsAndAdvancesAllocation() {
        helper.createDatabase("migration-test.db",1).apply {
            execSQL("INSERT INTO tasks VALUES ('t','Promise','Description',0,1000,1000,NULL)")
            execSQL("INSERT INTO graves VALUES ('t:1000','t',1000,-3.5,-4,0,0,2000,1,'neutral',0)")
            close()
        }
        helper.runMigrationsAndValidate("migration-test.db",2,true,Migrations.FROM_1_TO_2).apply {
            query("SELECT title FROM tasks").use {assertTrue(it.moveToFirst());assertEquals("Promise",it.getString(0))}
            query("SELECT COUNT(*) FROM graves").use{it.moveToFirst();assertEquals(1,it.getInt(0))}
            query("SELECT nextOrdinal FROM placement").use{it.moveToFirst();assertEquals(16,it.getInt(0))}
            close()
        }
    }
    @Test fun horizontalLayoutMigrationPreservesAllUserRecords() {
        helper.createDatabase("layout-migration-test.db",2).apply {
            execSQL("INSERT INTO tasks VALUES ('t','Untouched title','Description',0,1000,99999,NULL)")
            execSQL("INSERT INTO history VALUES ('h','t',2000,'rescheduled',1000,99999)")
            execSQL("INSERT INTO placement VALUES (1,120)")
            repeat(40){i->execSQL("INSERT INTO graves VALUES (?,?,?,?,?,?,?,?,?,?,?)",arrayOf<Any>("g$i","t",1000+i,-3.5,-4.0-i*3, i/16,i%3,2000+i,i%24,"philosophical",i%2))}
            close()
        }
        helper.runMigrationsAndValidate("layout-migration-test.db",3,true,Migrations.FROM_2_TO_3).apply {
            query("SELECT * FROM graves ORDER BY createdAt,id").use { rows ->
                var ordinal=0
                while(rows.moveToNext()){
                    val plot=org.cemetery.app.domain.CemeteryPlacement.plot(ordinal)
                    assertEquals("g$ordinal",rows.getString(rows.getColumnIndexOrThrow("id")))
                    assertEquals("t",rows.getString(rows.getColumnIndexOrThrow("taskId")))
                    assertEquals(plot.x,rows.getFloat(rows.getColumnIndexOrThrow("x")),.00001f)
                    assertEquals(plot.z,rows.getFloat(rows.getColumnIndexOrThrow("z")),.00001f)
                    assertEquals(plot.chunk,rows.getInt(rows.getColumnIndexOrThrow("chunk")))
                    assertEquals(ordinal%24,rows.getInt(rows.getColumnIndexOrThrow("epitaphId")))
                    assertEquals("philosophical",rows.getString(rows.getColumnIndexOrThrow("style")))
                    assertEquals(ordinal%2,rows.getInt(rows.getColumnIndexOrThrow("resurrected")))
                    ordinal++
                };assertEquals(40,ordinal)
            }
            query("SELECT title,deadline FROM tasks").use{it.moveToFirst();assertEquals("Untouched title",it.getString(0));assertEquals(99999L,it.getLong(1))}
            query("SELECT COUNT(*) FROM history").use{it.moveToFirst();assertEquals(1,it.getInt(0))}
            query("SELECT nextOrdinal FROM placement").use{it.moveToFirst();assertEquals(40,it.getInt(0))}
            close()
        }
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val reopened=androidx.room.Room.databaseBuilder(context,CemeteryDatabase::class.java,"layout-migration-test.db").addMigrations(Migrations.FROM_2_TO_3).build()
        kotlinx.coroutines.runBlocking {
            val records=reopened.dao().graves()
            assertEquals(40,records.size)
            records.forEachIndexed{i,g->assertEquals(org.cemetery.app.domain.CemeteryPlacement.plot(i).x,g.x,.00001f)}
        }
        reopened.close()
    }
    @Test fun upgradeFromFirstVersionKeepsAssociations() {
        helper.createDatabase("full-chain-test.db",1).apply {
            execSQL("INSERT INTO tasks VALUES ('old','Old promise','',0,1000,1000,2000)")
            execSQL("INSERT INTO graves VALUES ('old:1000','old',1000,7,-200,10,2,2000,9,'dark_humor',1)")
            close()
        }
        helper.runMigrationsAndValidate("full-chain-test.db",3,true,Migrations.FROM_1_TO_2,Migrations.FROM_2_TO_3).apply {
            query("SELECT id,taskId,epitaphId,resurrected FROM graves").use{assertTrue(it.moveToFirst());assertEquals("old:1000",it.getString(0));assertEquals("old",it.getString(1));assertEquals(9,it.getInt(2));assertEquals(1,it.getInt(3))}
            close()
        }
    }
}
