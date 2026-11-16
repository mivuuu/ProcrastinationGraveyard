package org.cemetery.app

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import androidx.test.runner.lifecycle.*
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.cemetery.app.feature.notifications.WorkerDependencies
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class CustomUiTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val context=instrumentation.targetContext
    private val device=UiDevice.getInstance(instrumentation)
    private val deps get()=EntryPointAccessors.fromApplication(context,WorkerDependencies::class.java)
    private fun click(text:String){
        repeat(4){attempt->
            val node=device.wait(Until.findObject(By.text(text)),15000);assertNotNull("Missing $text",node)
            try{node.click();device.waitForIdle();return}catch(e:StaleObjectException){if(attempt==3)throw e;android.os.SystemClock.sleep(150)}
        }
    }
    private fun description(text:String){val node=device.wait(Until.findObject(By.desc(text)),15000);assertNotNull(text,node);node.click();device.waitForIdle()}
    private fun screenshot(name:String){android.os.SystemClock.sleep(500);device.takeScreenshot(File(context.getExternalFilesDir(null),"custom-$name.png"))}
    private fun launch(){context.startActivity(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK));device.waitForIdle();android.os.SystemClock.sleep(500)}
    private fun activity():android.app.Activity?{var result:android.app.Activity?=null;instrumentation.runOnMainSync{result=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).firstOrNull()};return result}
    private fun exportCaptures(){
        val target="/sdcard/Download/cemetery-qa-${BuildConfig.VERSION_NAME}"
        device.executeShellCommand("mkdir -p $target")
        context.getExternalFilesDir(null)?.listFiles()?.filter{it.extension in listOf("png","mp4") && it.name.matches(Regex("[a-zA-Z0-9_.-]+"))}?.forEach{
            device.executeShellCommand("cp ${it.path} $target/${it.name}")
        }
    }
    @After fun exportEvidence(){exportCaptures()}
    @Before fun configure(){Configurator.getInstance().setWaitForIdleTimeout(500)}
    @get:Rule val failure=object:org.junit.rules.TestWatcher(){override fun failed(e:Throwable,d:org.junit.runner.Description){screenshot("failure-${d.methodName}")}}

    @Test fun referenceThemesAndLiveThemeState()=runBlocking<Unit>{
        val prefs=deps.preferences();val original=prefs.settings.first()
        try {
            prefs.text("language","en");prefs.text("theme","light");launch()
            assertTrue(device.wait(Until.hasObject(By.text("A place for a fresh start")),15000))
            screenshot("light-en")
            prefs.text("language","ru")
            assertTrue(device.wait(Until.hasObject(By.text("Место для нового начала")),15000));screenshot("light-ru")
            val before=activity();assertNotNull(before)
            description("Настройки");click("Светлое");click("Тёмное")
            assertTrue(device.wait(Until.hasObject(By.text("Тёмное")),5000))
            assertSame("Theme change recreated the activity",before,activity());screenshot("settings-dark-ru")
            device.pressBack();assertTrue(device.wait(Until.hasObject(By.text("Место для нового начала")),5000));screenshot("dark-ru")
            prefs.text("language","en");assertTrue(device.wait(Until.hasObject(By.text("A place for a fresh start")),15000));screenshot("dark-en")
            click("Completed");assertTrue(device.wait(Until.hasObject(By.text("Nothing here yet")),5000))
            click("Cemetery");click("Tasks")
            assertTrue("Task filter state was lost",device.wait(Until.hasObject(By.text("Nothing here yet")),5000))
            click("Active");description("Settings");click("Dark");click("Light")
            screenshot("settings-light-en");device.swipe(device.displayWidth/2,device.displayHeight*3/4,device.displayWidth/2,device.displayHeight/4,40);device.swipe(device.displayWidth/2,device.displayHeight*3/4,device.displayWidth/2,device.displayHeight/3,30);click("Open-source licenses")
            screenshot("licenses");device.pressBack();device.pressBack()
        } finally {device.pressHome();prefs.text("theme",original.theme);prefs.text("language",original.language)}
    }
    @Test fun customEditorValidationKeyboardAndPersistence()=runBlocking<Unit>{
        val prefs=deps.preferences();val original=prefs.settings.first();val repo=deps.repository()
        val name="Custom UI draft"
        try {
            prefs.text("language","en");prefs.text("theme","light");launch();click("Plant a promise")
            click("Save");assertTrue(device.wait(Until.hasObject(By.text(context.getString(R.string.validation_title))),5000))
            val field=device.findObjects(By.clazz("android.widget.EditText")).first();field.click();field.text=name
            screenshot("editor-keyboard");device.pressBack();screenshot("editor")
            click("Choose date");assertTrue(device.wait(Until.hasObject(By.desc("Previous month")),5000));screenshot("date-picker");click("Save")
            assertTrue(device.wait(Until.hasObject(By.text(context.getString(R.string.description))),5000))
            click("Choose time");assertTrue(device.wait(Until.hasObject(By.text("Hour")),5000));screenshot("time-picker");click("Save")
            assertTrue(device.wait(Until.hasObject(By.text(context.getString(R.string.description))),5000));click("Save")
            assertTrue(device.wait(Until.hasObject(By.text(name)),5000));screenshot("task-card")
            val task=repo.snapshot.first().tasks.first{it.title==name}
            launch();assertTrue(device.wait(Until.hasObject(By.text(name)),10000))
            assertEquals(task,repo.snapshot.first().tasks.first{it.id==task.id})
            click(name);val title=device.findObjects(By.clazz("android.widget.EditText")).first();title.click();title.text="$name edited"
            device.pressBack();click("Save");assertTrue(device.wait(Until.hasObject(By.text("$name edited")),5000))
            description("Mark completed");click("Completed");assertTrue(device.wait(Until.hasObject(By.text("$name edited")),5000))
            description("Delete task");click("Cancel")
            assertTrue("Confirmation did not finish its exit",device.wait(Until.gone(By.text("Cancel")),5000))
            assertTrue(device.wait(Until.hasObject(By.text("$name edited")),5000))
            assertEquals("Cancel deleted the persisted task",task.id,repo.snapshot.first().tasks.first{it.title=="$name edited"}.id)
            description("Delete task");screenshot("delete-confirmation");click("Delete task")
            assertTrue(device.wait(Until.gone(By.text("$name edited")),5000))
        } finally {
            device.pressHome();repo.snapshot.first().tasks.filter{it.title.startsWith(name)}.forEach{repo.delete(it.id)}
            prefs.text("theme",original.theme);prefs.text("language",original.language)
        }
    }
    @Test fun narrowScreenLargeTextAndKeyboardRemainUsable()=runBlocking<Unit>{
        val prefs=deps.preferences();val original=prefs.settings.first();val repo=deps.repository()
        val oldFont=device.executeShellCommand("settings get system font_scale").trim().takeIf{it!="null"}?:"1.0"
        val oldIme=device.executeShellCommand("settings get secure show_ime_with_hard_keyboard").trim().takeIf{it!="null"}?:"0"
        val oldSize=Regex("Override size: (\\d+x\\d+)").find(device.executeShellCommand("wm size"))?.groupValues?.get(1)
        try {
            prefs.text("language","en");prefs.text("theme","dark")
            device.executeShellCommand("wm size 880x1760") // 320 dp at this QA emulator's 440 dpi.
            device.executeShellCommand("settings put system font_scale 2.0")
            device.executeShellCommand("settings put secure show_ime_with_hard_keyboard 1")
            launch();assertTrue(device.wait(Until.hasObject(By.text("Plant a promise")),10000));screenshot("narrow-large-text")
            click("Plant a promise")
            val field=device.wait(Until.findObject(By.clazz("android.widget.EditText")),5000);assertNotNull(field);field.click();field.text="Narrow UI fixture"
            // Require actual IME visibility, rather than calling a focused field a keyboard check.
            var shown=false
            repeat(20){if(!shown){shown=device.executeShellCommand("dumpsys input_method").contains("mInputShown=true");if(!shown)android.os.SystemClock.sleep(150)}}
            assertTrue("Software keyboard was not requested",shown)
            assertTrue("Software keyboard did not draw",device.wait(Until.hasObject(By.pkg("com.android.inputmethod.latin")),10000));android.os.SystemClock.sleep(1000);screenshot("narrow-keyboard")
            device.pressBack();device.waitForIdle()
            repeat(8){if(!device.hasObject(By.text("Save")))device.swipe(device.displayWidth/2,device.displayHeight*3/4,device.displayWidth/2,device.displayHeight/3,35)}
            click("Save");kotlinx.coroutines.withTimeout(5000){repo.snapshot.first{snapshot->snapshot.tasks.any{it.title=="Narrow UI fixture"}}}
            device.setOrientationLeft();android.os.SystemClock.sleep(1000)
            assertTrue(device.wait(Until.hasObject(By.text("Plant a promise")),10000));screenshot("narrow-landscape")
        } finally {
            device.pressHome();device.setOrientationNatural();device.unfreezeRotation()
            device.executeShellCommand(if(oldSize==null)"wm size reset" else "wm size $oldSize")
            device.executeShellCommand("settings put system font_scale $oldFont")
            device.executeShellCommand("settings put secure show_ime_with_hard_keyboard $oldIme")
            repo.snapshot.first().tasks.filter{it.title=="Narrow UI fixture"}.forEach{repo.delete(it.id)}
            prefs.text("theme",original.theme);prefs.text("language",original.language)
        }
    }

}
