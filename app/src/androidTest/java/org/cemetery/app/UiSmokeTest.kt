package org.cemetery.app
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.*
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.cemetery.app.feature.notifications.WorkerDependencies
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import io.github.sceneview.math.toColumnsFloatArray
import org.cemetery.app.feature.cemetery.StoneSpec
@RunWith(AndroidJUnit4::class)
class UiSmokeTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val context=instrumentation.targetContext
    private val device=UiDevice.getInstance(instrumentation)
    private val deps get()=EntryPointAccessors.fromApplication(context,WorkerDependencies::class.java)
    @Before fun prepareAutomation(){Configurator.getInstance().setWaitForIdleTimeout(1000)}
    @get:Rule val failureScreenshot=object:org.junit.rules.TestWatcher(){
        override fun failed(error:Throwable,description:org.junit.runner.Description){device.takeScreenshot(File(context.getExternalFilesDir(null),"failure-${description.methodName}.png"))}
    }
    private fun sceneOnMain(block:(org.cemetery.app.feature.cemetery.CemeteryTextureView)->Unit) {
        var scene:org.cemetery.app.feature.cemetery.CemeteryTextureView?=null
        val until=android.os.SystemClock.uptimeMillis()+10_000
        do {
            instrumentation.runOnMainSync {
                fun find(view:android.view.View):org.cemetery.app.feature.cemetery.CemeteryTextureView? {
                    if(view is org.cemetery.app.feature.cemetery.CemeteryTextureView)return view
                    if(view is android.view.ViewGroup)for(i in 0 until view.childCount)find(view.getChildAt(i))?.let{return it}
                    return null
                }
                val activities=androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)
                scene=activities.mapNotNull{find(it.window.decorView)}.lastOrNull()
            }
            if(scene==null)android.os.SystemClock.sleep(50)
        }while(scene==null && android.os.SystemClock.uptimeMillis()<until)
        checkNotNull(scene){"No resumed cemetery SceneView"}
        instrumentation.runOnMainSync{block(scene!!)}
    }
    private fun cameraState():FloatArray {
        var result=FloatArray(7)
        sceneOnMain { val c=it.cameraNode;result=floatArrayOf(c.position.x,c.position.y,c.position.z,c.rotation.x,c.rotation.y,c.rotation.z,(c.focalLength*1000).toFloat()) }
        return result
    }
    private fun tapGrave(grave:org.cemetery.app.domain.Grave) {
        var x=0;var y=0
        sceneOnMain { scene ->
            val p=scene.cameraNode.worldToView(io.github.sceneview.math.Position(grave.x,StoneSpec(grave).height*.82f,grave.z+.27f))
            val origin=IntArray(2);scene.getLocationOnScreen(origin)
            x=origin[0]+(p.x*scene.width).toInt();y=origin[1]+((1-p.y)*scene.height).toInt()
        }
        device.click(x,y)
    }
    private fun assertForeground(grave:org.cemetery.app.domain.Grave) {
        var bottomPixel=0
        sceneOnMain{scene->
            val root=scene.childNodes.first{it.name=="miniature:${grave.id}"}
            val bottom=scene.cameraNode.worldToView(io.github.sceneview.math.Position(root.position.x,root.position.y-.15f*root.scale.y,root.position.z+.5f*root.scale.z))
            val origin=IntArray(2);scene.getLocationOnScreen(origin);bottomPixel=origin[1]+((1-bottom.y)*scene.height).toInt()
            val p=scene.cameraNode.worldToView(io.github.sceneview.math.Position(root.position.x,root.position.y+StoneSpec(grave).height*root.scale.y/2,root.position.z))
            assertEquals(.5f,p.x,.02f);assertTrue("Presentation is obscured: $p",p.y in .67f.. .94f)
            assertTrue(scene.childNodes.any{it.name=="${if(grave.resurrected)"halo" else "outline"}:${grave.id}"})
            val m=root.worldTransform.toColumnsFloatArray();assertEquals("Stone did not stop facing front",1f,m[10]/root.scale.x,.02f)
        }
        val sheet=device.wait(Until.findObject(By.res("grave-sheet")),5000)
        assertNotNull("Missing task sheet bounds",sheet)
        val close=device.wait(Until.findObject(By.res("grave-close")),5000)
        assertNotNull("Close must remain visible without scrolling",close)
        val density=context.resources.displayMetrics.density
        assertTrue("Close button is clipped",close.visibleBounds.height()>=48*density-2)
        assertTrue("Close has no bottom breathing space",sheet.visibleBounds.bottom-close.visibleBounds.bottom>=12*density)
        var navigationBottom=0
        instrumentation.runOnMainSync{
            val host=androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED).firstOrNull()
            navigationBottom=host?.window?.decorView?.let{androidx.core.view.ViewCompat.getRootWindowInsets(it)?.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars())?.bottom}?:0
        }
        assertTrue("Task sheet extends under system navigation",sheet.visibleBounds.bottom<=device.displayHeight-navigationBottom+2)
        assertTrue("Foreground stone is covered by task sheet: $bottomPixel >= ${sheet.visibleBounds.top}",bottomPixel+5<sheet.visibleBounds.top)
    }
    private fun assertRestored(grave:org.cemetery.app.domain.Grave,camera:FloatArray) {
        var returned=false
        val until=android.os.SystemClock.uptimeMillis()+5000
        do{
            sceneOnMain{scene->val root=scene.childNodes.firstOrNull{it.name=="miniature:${grave.id}"};returned=root!=null && kotlin.math.abs(root.position.x-grave.x)<.0001f && kotlin.math.abs(root.position.y)<.0001f && kotlin.math.abs(root.position.z-grave.z)<.0001f && scene.childNodes.none{it.name=="outline:${grave.id}"}}
            if(!returned)android.os.SystemClock.sleep(50)
        }while(!returned && android.os.SystemClock.uptimeMillis()<until)
        assertTrue("Selected stone never returned",returned)
        val actual=cameraState();for(i in 0..6)assertEquals("Camera changed after dismissal",camera[i],actual[i],.003f)
        sceneOnMain{scene->
            val root=scene.childNodes.first{it.name=="miniature:${grave.id}"}
            assertEquals(grave.x,root.position.x,.0001f);assertEquals(0f,root.position.y,.0001f);assertEquals(grave.z,root.position.z,.0001f);assertEquals(1f,root.scale.x,.0001f)
            val m=root.worldTransform.toColumnsFloatArray();assertEquals(1f,m[10],.001f);assertEquals(0f,m[8],.001f)
            assertFalse(scene.childNodes.any{it.name=="outline:${grave.id}"})
        }
    }
    private fun pinch(ratio:Float) {
        var cx=0f;var cy=0f
        sceneOnMain { scene -> val origin=IntArray(2);scene.getLocationOnScreen(origin);cx=origin[0]+scene.width*.5f;cy=origin[1]+scene.height*.55f }
        val start=android.os.SystemClock.uptimeMillis()
        val properties=Array(2){i->android.view.MotionEvent.PointerProperties().apply{id=i;toolType=android.view.MotionEvent.TOOL_TYPE_FINGER}}
        fun send(action:Int,count:Int,halfSpan:Float){
            val coords=Array(2){i->android.view.MotionEvent.PointerCoords().apply{x=cx+(if(i==0)-halfSpan else halfSpan);y=cy;pressure=1f;size=1f}}
            val event=android.view.MotionEvent.obtain(start,android.os.SystemClock.uptimeMillis(),action,count,properties,coords,0,0,1f,1f,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0)
            instrumentation.uiAutomation.injectInputEvent(event,true);event.recycle()
        }
        send(android.view.MotionEvent.ACTION_DOWN,1,100f)
        send(android.view.MotionEvent.ACTION_POINTER_DOWN or (1 shl android.view.MotionEvent.ACTION_POINTER_INDEX_SHIFT),2,100f)
        repeat(16){i->android.os.SystemClock.sleep(18);send(android.view.MotionEvent.ACTION_MOVE,2,100f*(1+(ratio-1)*(i+1)/16))}
        send(android.view.MotionEvent.ACTION_POINTER_UP or (1 shl android.view.MotionEvent.ACTION_POINTER_INDEX_SHIFT),2,100f*ratio)
        send(android.view.MotionEvent.ACTION_UP,1,100f*ratio)
    }
    private fun clickDescription(description:String){
        repeat(4){attempt->
            val node=device.wait(Until.findObject(By.desc(description)),30_000)
            assertNotNull("Missing $description",node)
            try{node.click();device.waitForIdle();return}catch(e:StaleObjectException){if(attempt==3)throw e;android.os.SystemClock.sleep(150)}
        }
    }
    private fun click(text:String){
        repeat(4){attempt->
            if(!device.wait(Until.hasObject(By.text(text)),30_000)){
                device.takeScreenshot(File(context.getExternalFilesDir(null),"missing.png"))
                device.dumpWindowHierarchy(File(context.getExternalFilesDir(null),"missing.xml"))
                fail("Missing $text")
            }
            try{device.findObjects(By.text(text)).last().click();device.waitForIdle();return}catch(e:StaleObjectException){if(attempt==3)throw e;android.os.SystemClock.sleep(150)}
        }
    }
    private fun waitForGeometry(name:String) {
        sceneOnMain{}
        assertTrue(device.wait(Until.gone(By.text("Preparing the garden…")),30_000))
        assertTrue(device.wait(Until.gone(By.text("Подготавливаем сад…")),30_000))
        val imageFile=File(context.getExternalFilesDir(null),name)
        val renderTimeout=System.currentTimeMillis()+30_000
        var colorCount=0
        do {
            android.os.SystemClock.sleep(1000)
            device.takeScreenshot(imageFile)
            val bitmap=android.graphics.BitmapFactory.decodeFile(imageFile.path)
            val colors=mutableSetOf<Int>()
            for(y in bitmap.height*3/10 until bitmap.height*6/10 step 8)for(x in bitmap.width/8 until bitmap.width*7/8 step 8)colors+=bitmap.getPixel(x,y)
            bitmap.recycle();colorCount=colors.size
        } while(colorCount<=20 && System.currentTimeMillis()<renderTimeout)
        assertTrue("3D scene has no visible geometry",colorCount>20)
    }
    @Test fun realScreensAndPersistedSettings()=runBlocking<Unit> {
        val prefs=deps.preferences();prefs.text("language","en");prefs.text("theme","light")
        val repo=deps.repository()
        // Fixtures cover all four rows and multiple horizontal sections; never shipped in the app.
        repeat(36){repo.save("ui-smoke-$it","Promise ${it+1}","A quiet test commitment",System.currentTimeMillis()-86_400_000-it*60_000)}
        context.startActivity(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        click("Cemetery")
        assertTrue(device.wait(Until.hasObject(By.desc("Browse grave records")),10_000))
        assertTrue(device.wait(Until.gone(By.text("Preparing the garden…")),30_000))
        waitForGeometry("cemetery.png")
        assertFalse(device.hasObject(By.desc("Return to entrance")))
        assertFalse(device.hasObject(By.desc("Move forward")))
        sceneOnMain { scene ->
            assertTrue(scene.childNodes.any{it.name?.startsWith("forest:")==true})
            assertTrue(scene.childNodes.size<1400)
            fun count(node:io.github.sceneview.node.Node):Int=(if(node is io.github.sceneview.node.RenderableNode)1 else 0)+node.childNodes.sumOf{count(it)}
            val renderables=scene.childNodes.sumOf{count(it)}
            File(context.getExternalFilesDir(null),"scene-budget.txt").writeText("Visible renderable nodes: $renderables; seeded historical graves: 36\n")
            assertTrue("Active scene exceeded the mobile renderable budget: $renderables",renderables<400)
        }
        val before=cameraState()
        device.swipe(device.displayWidth/2,1100,device.displayWidth/2-100,1100,20)
        android.os.SystemClock.sleep(1800)
        val horizontal=cameraState()
        assertTrue(kotlin.math.abs(before[0]-horizontal[0])>.05f)
        for(i in 1..5)assertEquals(before[i],horizontal[i],.001f)
        device.swipe(device.displayWidth/2,900,device.displayWidth/2,1250,20)
        android.os.SystemClock.sleep(1200)
        val vertical=cameraState()
        assertEquals(horizontal[0],vertical[0],.03f)
        assertEquals(org.cemetery.app.feature.cemetery.CameraController.HEIGHT,vertical[1],.001f)
        assertEquals(org.cemetery.app.feature.cemetery.CameraController.DEPTH,vertical[2],.001f)
        pinch(1.35f);android.os.SystemClock.sleep(1600)
        val zoomed=cameraState()
        assertTrue("Native lens did not zoom: ${vertical[6]} -> ${zoomed[6]} mm",zoomed[6]>vertical[6]+2)
        for(i in 0..5)assertEquals(vertical[i],zoomed[i],.03f)
        assertFalse(device.hasObject(By.text("A promise remembered")))
        val loaded=repo.snapshot.first()
        val allGraves=loaded.graves
        val grave=allGraves.filter{it.z>1.9f}.minBy{kotlin.math.abs(it.x-zoomed[0])}
        val original=grave
        // Observe every native scene frame; screenshot acquisition must not skip spin samples.
        val samples=java.util.Collections.synchronizedList(mutableListOf<FloatArray>())
        val backVisible=java.util.concurrent.CountDownLatch(1)
        sceneOnMain{scene->
            val root=scene.childNodes.first{it.name=="miniature:${grave.id}"}
            root.onFrame={
                val c=scene.cameraNode;val m=root.worldTransform.toColumnsFloatArray();val scale=root.scale.x
                if(m[10]/scale<-.8f)backVisible.countDown()
                samples+=floatArrayOf(c.position.x,c.position.y,c.position.z,c.rotation.x,c.rotation.y,c.rotation.z,(c.focalLength*1000).toFloat(),root.position.y,m[8]/scale,m[10]/scale,root.position.z,scale)
            }
        }
        tapGrave(grave)
        assertTrue("Back face was never reached",backVisible.await(3,java.util.concurrent.TimeUnit.SECONDS))
        assertFalse("Information opened before the spin completed",device.hasObject(By.text("A promise remembered")))
        android.os.SystemClock.sleep(1400)
        sceneOnMain{scene->scene.childNodes.first{it.name=="miniature:${grave.id}"}.onFrame=null}
        File(context.getExternalFilesDir(null),"native-spin.csv").writeText(samples.joinToString("\n"){it.joinToString(",")})
        samples.forEach{native->for(i in 0..6)assertEquals("Camera moved during object spin, component $i",zoomed[i],native[i],.003f)}
        assertTrue("The actual stone never showed its right side",samples.any{it[8]>.8f})
        assertTrue("The actual stone never showed its back",samples.any{it[9]<-.8f})
        assertTrue("The actual stone never showed its left side",samples.any{it[8]<-.8f})
        assertTrue("The stone did not fly into the foreground",samples.any{it[7]>3f && it[10]>10f})
        assertTrue("The task panel never opened",device.wait(Until.hasObject(By.text("A promise remembered")),5000))
        assertForeground(grave)
        device.takeScreenshot(File(context.getExternalFilesDir(null),"focused-zoom.png"))
        device.pressBack();android.os.SystemClock.sleep(900)
        assertRestored(grave,zoomed)
        assertEquals(original,repo.snapshot.first().graves.first{it.id==grave.id})
        pinch(.55f);android.os.SystemClock.sleep(1800)
        val wide=cameraState()
        for(row in 0..3){
            val bandZ=org.cemetery.app.domain.CemeteryPlacement.FRONT_Z-row*org.cemetery.app.domain.CemeteryPlacement.ROW_SPACING
            val candidate=allGraves.filter{kotlin.math.abs(it.z-bandZ)<.26f}.minBy{kotlin.math.abs(it.x-wide[0])}
            tapGrave(candidate)
            assertTrue("Cannot select row $row",device.wait(Until.hasObject(By.text("A promise remembered")),6000))
            val title=loaded.tasks.first{it.id==candidate.taskId}.title
            assertTrue("Wrong grave selected in row $row",device.wait(Until.hasObject(By.text(title)),5000))
            assertForeground(candidate)
            device.takeScreenshot(File(context.getExternalFilesDir(null),"row-$row.png"))
            for(i in 0..6)assertEquals(wide[i],cameraState()[i],.003f)
            device.pressBack();android.os.SystemClock.sleep(900)
            assertRestored(candidate,wide)
        }
        clickDescription("Browse grave records");click("Promise 1")
        assertTrue(device.wait(Until.hasObject(By.text("A promise remembered")),10_000))
        assertForeground(repo.snapshot.first().graves.first{it.taskId=="ui-smoke-0"})
        device.takeScreenshot(File(context.getExternalFilesDir(null),"selected.png"))
        repo.complete("ui-smoke-0")
        assertTrue(device.wait(Until.hasObject(By.text("Status: Resurrected")),5000))
        android.os.SystemClock.sleep(3000)
        device.takeScreenshot(File(context.getExternalFilesDir(null),"flower.png"))
        device.pressBack()
        clickDescription("Settings");click("English");click("Russian")
        assertTrue(device.wait(Until.hasObject(By.text("Язык")),10_000))
        click("Светлое");click("Тёмное")
        assertTrue(device.wait(Until.gone(By.text("Светлое")),15_000))
        android.os.SystemClock.sleep(1200)
        device.takeScreenshot(File(context.getExternalFilesDir(null),"settings-ru-dark.png"))
        assertEquals("ru",prefs.settings.first().language);assertEquals("dark",prefs.settings.first().theme)
        context.startActivity(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        assertTrue(device.wait(Until.hasObject(By.text("Задачи")),10_000))
        click("Кладбище")
        assertTrue(device.wait(Until.hasObject(By.desc("Список надгробий")),10_000))
        assertTrue(device.wait(Until.gone(By.text("Подготавливаем сад…")),30_000))
        waitForGeometry("cemetery-dark.png")
    }
    @Test fun earlyCancellationAndTabChangesRestoreWorld()=runBlocking<Unit>{
        deps.preferences().text("language","en");deps.preferences().text("theme","light")
        val repo=deps.repository()
        repeat(8){repo.save("ui-smoke-cancel-$it","Cancel promise $it","",System.currentTimeMillis()-60_000)}
        context.startActivity(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        click("Cemetery");waitForGeometry("cancellation-before.png")
        val camera=cameraState()
        val grave=repo.snapshot.first().graves.filter{it.z>1.9f}.minBy{kotlin.math.abs(it.x-camera[0])}
        tapGrave(grave)
        var lifted=false
        val liftTimeout=android.os.SystemClock.uptimeMillis()+1500
        do{sceneOnMain{scene->lifted=scene.childNodes.first{it.name=="miniature:${grave.id}"}.position.y>.01f};if(!lifted)android.os.SystemClock.sleep(25)}while(!lifted && android.os.SystemClock.uptimeMillis()<liftTimeout)
        assertTrue("Cancellation fixture was not selected",lifted)
        device.pressBack();android.os.SystemClock.sleep(1000)
        assertFalse(device.hasObject(By.text("A promise remembered")));assertRestored(grave,camera)
        tapGrave(grave);android.os.SystemClock.sleep(150);click("Tasks");click("Cemetery")
        waitForGeometry("cancellation-after.png")
        assertRestored(grave,camera)
        assertEquals(grave,repo.snapshot.first().graves.first{it.id==grave.id})
        tapGrave(grave);android.os.SystemClock.sleep(200);device.pressHome();android.os.SystemClock.sleep(700)
        context.startActivity(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        assertTrue(device.wait(Until.hasObject(By.text("A promise remembered")),6000));assertForeground(grave)
        device.pressBack();assertRestored(grave,camera)
    }
    @Test fun largeTextAndLandscapeKeepPresentationVisible()=runBlocking<Unit>{
        val oldFont=device.executeShellCommand("settings get system font_scale").trim().takeIf{it!="null"}?:"1.0"
        try{
            device.executeShellCommand("settings put system font_scale 2.0")
            deps.preferences().text("language","en");deps.preferences().text("theme","light")
            val repo=deps.repository()
            repeat(16){repo.save("ui-smoke-size-$it","Size promise $it","",System.currentTimeMillis()-60_000)}
            context.startActivity(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            click("Cemetery");waitForGeometry("large-text.png")
            val cam=cameraState()
            val grave=repo.snapshot.first().graves.filter{it.z>1.9f}.minBy{kotlin.math.abs(it.x-cam[0])}
            tapGrave(grave);assertTrue(device.wait(Until.hasObject(By.text("A promise remembered")),6000));assertForeground(grave)
            device.takeScreenshot(File(context.getExternalFilesDir(null),"large-text-selected.png"));device.pressBack();assertRestored(grave,cam)
            device.setOrientationLeft();android.os.SystemClock.sleep(1500);waitForGeometry("landscape.png")
            val wide=cameraState();tapGrave(grave)
            assertTrue(device.wait(Until.hasObject(By.text("A promise remembered")),6000));assertForeground(grave)
            device.takeScreenshot(File(context.getExternalFilesDir(null),"landscape-selected.png"));device.pressBack();assertRestored(grave,wide)
        }finally{device.setOrientationNatural();device.unfreezeRotation();device.executeShellCommand("settings put system font_scale $oldFont")}
    }
    @Test fun recordedSpinAndReducedMotionKeepDataSafe()=runBlocking<Unit>{
        val oldScale=device.executeShellCommand("settings get global animator_duration_scale").trim().takeIf{it!="null"}?:"1.0"
        var recorder:Thread?=null
        try{
            device.executeShellCommand("settings put global animator_duration_scale 1.0")
            deps.preferences().text("language","en");deps.preferences().text("theme","light")
            val repo=deps.repository()
            repeat(4){repo.save("ui-smoke-demo-$it","A small promise $it","A local commitment",System.currentTimeMillis()-60_000)}
            context.startActivity(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            click("Cemetery");waitForGeometry("demo-before.png")
            val cam=cameraState()
            val grave=repo.snapshot.first().graves.filter{it.z>1.9f}.minBy{kotlin.math.abs(it.x-cam[0])}
            val movie=File(context.getExternalFilesDir(null),"cartoon-selection.mp4")
            recorder=Thread{device.executeShellCommand("screenrecord --size 480x1040 --bit-rate 1500000 --time-limit 12 ${movie.path}")}.also{it.start()}
            android.os.SystemClock.sleep(900)
            tapGrave(grave);assertTrue(device.wait(Until.hasObject(By.text("A promise remembered")),6000));assertForeground(grave)
            device.takeScreenshot(File(context.getExternalFilesDir(null),"demo-selected.png"))
            android.os.SystemClock.sleep(500);device.pressBack();assertRestored(grave,cam)
            recorder.join(15_000);assertFalse("Screen recording did not finish",recorder.isAlive);assertTrue("Missing actual scene recording",movie.length()>0)
            device.executeShellCommand("settings put global animator_duration_scale 0.0")
            var disabled=false
            val until=android.os.SystemClock.uptimeMillis()+5000
            do{instrumentation.runOnMainSync{disabled=!android.animation.ValueAnimator.areAnimatorsEnabled()};if(!disabled)android.os.SystemClock.sleep(50)}while(!disabled && android.os.SystemClock.uptimeMillis()<until)
            assertTrue("Reduced motion setting did not reach the app",disabled)
            tapGrave(grave);assertTrue(device.wait(Until.hasObject(By.text("A promise remembered")),2500));assertForeground(grave)
            device.takeScreenshot(File(context.getExternalFilesDir(null),"reduced-motion.png"))
            device.pressBack();assertRestored(grave,cam)
            assertEquals(grave,repo.snapshot.first().graves.first{it.id==grave.id})
        }finally{recorder?.join(15_000);device.executeShellCommand("settings put global animator_duration_scale $oldScale")}
    }
    @Test fun createCompleteAndDeleteThroughUi()=runBlocking<Unit> {
        deps.preferences().text("language","en")
        context.startActivity(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        click("Plant a promise")
        assertTrue(device.wait(Until.hasObject(By.clazz("android.widget.EditText")),5000))
        device.findObjects(By.clazz("android.widget.EditText")).first().apply{click();text="UI promise"}
        device.pressBack();click("Choose date")
        assertTrue(device.wait(Until.hasObject(By.desc("Previous month")),5000));click("Save")
        assertTrue(device.wait(Until.hasObject(By.text(context.getString(R.string.description))),5000))
        click("Choose time");assertTrue(device.wait(Until.hasObject(By.text("Hour")),5000));click("Save")
        assertTrue(device.wait(Until.hasObject(By.text(context.getString(R.string.description))),5000));click("Save")
        assertTrue(device.wait(Until.hasObject(By.text("UI promise")),5000))
        assertTrue(device.wait(Until.hasObject(By.desc("Mark completed")),5000))
        clickDescription("Mark completed");click("Completed")
        assertTrue(device.wait(Until.hasObject(By.text("UI promise")),5000))
        clickDescription("Delete task");click("Delete task")
        assertTrue(device.wait(Until.gone(By.text("UI promise")),5000))
    }
    private fun exportCaptures(){
        val target="/sdcard/Download/cemetery-qa-${BuildConfig.VERSION_NAME}"
        device.executeShellCommand("mkdir -p $target")
        context.getExternalFilesDir(null)?.listFiles()?.filter{it.extension in listOf("png","mp4") && it.name.matches(Regex("[a-zA-Z0-9_.-]+"))}?.forEach{
            device.executeShellCommand("cp ${it.path} $target/${it.name}")
        }
    }
    @After fun cleanFixtures()=runBlocking<Unit> {
        exportCaptures()
        device.takeScreenshot(File(context.getExternalFilesDir(null),"last-frame.png"))
        device.pressHome()
        deps.repository().snapshot.first().tasks.filter{it.id.startsWith("ui-smoke-") || it.title=="UI promise"}.forEach{deps.repository().delete(it.id)}
        deps.preferences().text("language","system");deps.preferences().text("theme","system")
    }
}
