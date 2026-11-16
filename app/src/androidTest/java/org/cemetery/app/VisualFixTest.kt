package org.cemetery.app

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.uiautomator.*
import dagger.hilt.android.EntryPointAccessors
import org.cemetery.app.feature.cemetery.CemeteryTextureView as SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.node.Node
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.cemetery.app.data.Settings
import org.cemetery.app.domain.Grave
import org.cemetery.app.feature.cemetery.StoneSpec
import org.cemetery.app.feature.cemetery.CemeteryViewportState
import org.cemetery.app.feature.cemetery.CemeteryRenderSession
import org.cemetery.app.feature.notifications.WorkerDependencies
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

/** Runs only on a disposable QA emulator; fixtures never ship in the application. */
@RunWith(AndroidJUnit4::class)
class VisualFixTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val context=instrumentation.targetContext
    private val device=UiDevice.getInstance(instrumentation)
    private val deps get()=EntryPointAccessors.fromApplication(context,WorkerDependencies::class.java)
    private var original:Settings?=null
    private var originalMotion="1.0"
    private val captures="/sdcard/Download/cemetery-qa-${BuildConfig.VERSION_NAME}"
    @Before fun prepare()=runBlocking<Unit>{
        Configurator.getInstance().setWaitForIdleTimeout(300)
        original=deps.preferences().settings.first()
        originalMotion=device.executeShellCommand("settings get global animator_duration_scale").trim().takeIf{it!="null"}?:"1.0"
        deps.preferences().text("language","en");deps.preferences().text("theme","light")
        device.executeShellCommand("mkdir -p $captures")
    }
    @get:Rule val failure=object:org.junit.rules.TestWatcher(){override fun failed(e:Throwable,d:org.junit.runner.Description){capture("failure-${d.methodName}")}}
    private fun capture(name:String){device.executeShellCommand("screencap -p $captures/$name.png")}
    private fun scenes():List<SceneView>{
        val found=mutableListOf<SceneView>()
        instrumentation.runOnMainSync{
            fun walk(v:View){if(v is SceneView)found+=v;if(v is ViewGroup)repeat(v.childCount){walk(v.getChildAt(it))}}
            ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).forEach{walk(it.window.decorView)}
        };return found
    }
    private fun onScene(block:(SceneView)->Unit){
        val until=SystemClock.uptimeMillis()+15000
        var scene:SceneView?=null
        do{scene=scenes().firstOrNull{it.isShown};if(scene==null)SystemClock.sleep(30)}while(scene==null && SystemClock.uptimeMillis()<until)
        assertNotNull("Missing visible cemetery viewport",scene)
        instrumentation.runOnMainSync{block(scene!!)}
    }
    private fun click(text:String){repeat(4){attempt->
        assertTrue("Missing $text",device.wait(Until.hasObject(By.text(text)),15000))
        try{device.findObjects(By.text(text)).last().click();device.waitForIdle();return}catch(e:StaleObjectException){if(attempt==3)throw e;SystemClock.sleep(80)}
    }}
    private fun launch(){context.startActivity(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK));assertTrue(device.wait(Until.hasObject(By.text("Tasks")),15000));click("Cemetery");ready()}
    private fun ready(){
        val until=SystemClock.uptimeMillis()+15000
        var rendered=false
        do{onScene{rendered=(it.tag as CemeteryViewportState).gate.ready};if(!rendered)SystemClock.sleep(30)}while(!rendered && SystemClock.uptimeMillis()<until)
        assertTrue("Surface did not submit a completed GPU frame",rendered)
        assertTrue(device.wait(Until.gone(By.text("Preparing the garden…")),15000));onScene{assertTrue(it.lifecycle!!.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED))}}
    private fun frame():FloatArray{var value=FloatArray(7);onScene{val c=it.cameraNode;value=floatArrayOf(c.position.x,c.position.y,c.position.z,c.rotation.x,c.rotation.y,c.rotation.z,(c.focalLength*1000).toFloat())};return value}
    private fun tap(g:Grave){var x=0;var y=0;onScene{v->val p=v.cameraNode.worldToView(Position(g.x,StoneSpec(g).height*.82f,g.z+.27f));val o=IntArray(2);v.getLocationOnScreen(o);x=o[0]+(p.x*v.width).toInt();y=o[1]+((1-p.y)*v.height).toInt()};device.click(x,y)}
    private fun inspect(){assertTrue(device.wait(Until.hasObject(By.res("grave-sheet")),6000))}
    private fun closeBounds():android.graphics.Rect{
        val sheet=device.wait(Until.findObject(By.res("grave-sheet")),5000);val close=device.wait(Until.findObject(By.res("grave-close")),5000)
        assertNotNull("Close must be visible without scrolling",close)
        val density=context.resources.displayMetrics.density
        assertTrue("Close is clipped: ${close.visibleBounds}, sheet=${sheet.visibleBounds}, density=$density",close.visibleBounds.height()>=48*density-2)
        assertTrue("Close has no comfortable bottom padding",sheet.visibleBounds.bottom-close.visibleBounds.bottom>=12*density)
        assertTrue("Close extends under system navigation",close.visibleBounds.bottom<device.displayHeight-12*density)
        return close.visibleBounds
    }
    private fun restored(g:Grave,camera:FloatArray){
        val until=SystemClock.uptimeMillis()+4000
        var done=false
        do{onScene{s->val n=s.childNodes.firstOrNull{it.name=="miniature:${g.id}"};done=n!=null && kotlin.math.abs(n.position.x-g.x)<.001f && kotlin.math.abs(n.position.y)<.001f && kotlin.math.abs(n.position.z-g.z)<.001f && s.childNodes.none{it.name=="outline:${g.id}" || it.name=="halo:${g.id}"}};if(!done)SystemClock.sleep(30)}while(!done && SystemClock.uptimeMillis()<until)
        assertTrue("Selection visuals or fly-out transform remained after dismissal",done)
        assertArrayEquals("Selection changed the camera",camera,frame(),.003f)
    }
    @Test fun flowersUseOneRoundHaloAndCloseIsFixed()=runBlocking<Unit>{
        val repo=deps.repository();val now=System.currentTimeMillis()
        repeat(4){repo.save("visual-fix-flower-$it","Flower promise $it","A long local description. ".repeat(12),now-60_000)}
        repo.complete("visual-fix-flower-0")
        val before=repo.snapshot.first();val flower=before.graves.first{it.taskId=="visual-fix-flower-0"}
        launch();val camera=frame()
        for(theme in listOf("light","dark")){
            deps.preferences().text("theme",theme)
            repeat(3){cycle->
                device.executeShellCommand("settings put global animator_duration_scale ${if(cycle==0)originalMotion else "0.0"}")
                val disabled=cycle>0 || originalMotion.toFloat()==0f
                val motionTimeout=SystemClock.uptimeMillis()+5000;var matches=false
                do{instrumentation.runOnMainSync{matches=(!android.animation.ValueAnimator.areAnimatorsEnabled())==disabled};if(!matches)SystemClock.sleep(30)}while(!matches && SystemClock.uptimeMillis()<motionTimeout)
                assertTrue("Motion preference did not reach the app",matches)
                tap(flower);inspect();val bounds=closeBounds()
                onScene{s->
                    assertEquals(1,s.childNodes.count{it.name=="halo:${flower.id}" && it.isVisible})
                    assertFalse(s.childNodes.any{it.name?.startsWith("outline:")==true})
                    val halo=s.childNodes.first{it.name=="halo:${flower.id}"}
                    val miniature=s.childNodes.first{it.name=="miniature:${flower.id}"}
                    assertTrue("Repeated selection did not fly into the foreground",miniature.position.y>1f && miniature.position.z>10f)
                    assertEquals(2,halo.childNodes.size)
                    assertTrue(halo.childNodes.all{!it.isHittable && !it.isTouchable})
                }
                val body=device.findObjects(By.scrollable(true)).lastOrNull{it.visibleBounds.top>=device.findObject(By.res("grave-sheet")).visibleBounds.top}
                assertNotNull("Missing scrollable details",body);body!!.scroll(Direction.DOWN,1f)
                assertEquals("Close moved with the scrolling content",bounds,closeBounds())
                if(cycle==0)capture("flower-$theme")
                device.findObject(By.res("grave-close")).click();restored(flower,camera)
            }
        }
        // Completion while a stone is selected must retire its contour, not stack a halo on it.
        val stone=before.graves.first{it.taskId=="visual-fix-flower-1"}
        tap(stone);inspect();onScene{s->assertEquals(1,s.childNodes.count{it.name=="outline:${stone.id}"})}
        repo.complete(stone.taskId)
        val until=SystemClock.uptimeMillis()+5000;var halo=false
        do{onScene{s->halo=s.childNodes.count{it.name=="halo:${stone.id}" && it.isVisible}==1 && s.childNodes.none{it.name?.startsWith("outline:")==true}};if(!halo)SystemClock.sleep(30)}while(!halo && SystemClock.uptimeMillis()<until)
        assertTrue("Completion left an old/duplicate contour",halo);closeBounds();capture("stone-to-flower")
        device.findObject(By.res("grave-close")).click();restored(stone,camera)
        assertEquals(before.graves.map{it.id},repo.snapshot.first().graves.map{it.id})
    }
    private fun cloudPoints():FloatArray{var points=FloatArray(4);onScene{s->
        val sky=s.childNodes.first{it.name=="cartoon-sky"}
        assertSame("Sky is not camera-relative",s.cameraNode,sky.parent)
        assertTrue(sky.childNodes.any{it.name=="cartoon-clouds"})
        assertTrue(s.childNodes.filter{it!==sky}.all{it.parent!==s.cameraNode})
        listOf(Position(-.65f,.70f,0f),Position(.82f,.65f,0f)).forEachIndexed{i,p->val v=s.cameraNode.worldToView(sky.getWorldPosition(p));points[i*2]=v.x;points[i*2+1]=v.y}
    };return points}
    private fun pan(left:Boolean){var x1=0;var x2=0;var y=0;onScene{s->val o=IntArray(2);s.getLocationOnScreen(o);x1=o[0]+s.width*(if(left)8 else 2)/10;x2=o[0]+s.width*(if(left)2 else 8)/10;y=o[1]+s.height/2};device.swipe(x1,y,x2,y,8)}
    private fun pinch(ratio:Float){
        var x=0f;var y=0f;onScene{s->val o=IntArray(2);s.getLocationOnScreen(o);x=o[0]+s.width*.5f;y=o[1]+s.height*.55f}
        val start=SystemClock.uptimeMillis()
        val properties=Array(2){i->android.view.MotionEvent.PointerProperties().apply{id=i;toolType=android.view.MotionEvent.TOOL_TYPE_FINGER}}
        fun send(action:Int,count:Int,span:Float){val coords=Array(2){i->android.view.MotionEvent.PointerCoords().apply{this.x=x+(if(i==0)-span else span);this.y=y;pressure=1f;size=1f}};val event=android.view.MotionEvent.obtain(start,SystemClock.uptimeMillis(),action,count,properties,coords,0,0,1f,1f,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0);instrumentation.uiAutomation.injectInputEvent(event,true);event.recycle()}
        send(android.view.MotionEvent.ACTION_DOWN,1,100f);send(android.view.MotionEvent.ACTION_POINTER_DOWN or (1 shl 8),2,100f)
        repeat(12){i->SystemClock.sleep(18);send(android.view.MotionEvent.ACTION_MOVE,2,100f*(1+(ratio-1)*(i+1)/12))}
        send(android.view.MotionEvent.ACTION_POINTER_UP or (1 shl 8),2,100f*ratio);send(android.view.MotionEvent.ACTION_UP,1,100f*ratio)
    }
    private fun awaitNativeFrame(){
        // Accessibility can update before HWUI submits the destination's first buffer.
        // Await an actual window frame rather than sleeping or polling for a desired pixel color.
        val committed=java.util.concurrent.CountDownLatch(1)
        instrumentation.runOnMainSync{
            val root=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).first().window.decorView
            root.viewTreeObserver.registerFrameCommitCallback{committed.countDown()}
            root.postInvalidateOnAnimation()
        }
        assertTrue("Native destination did not commit a frame",committed.await(5,java.util.concurrent.TimeUnit.SECONDS))
        device.waitForIdle()
    }
    private fun assertTasksHaveNoSurface(name:String){
        assertTrue(device.wait(Until.hasObject(By.text(context.getString(R.string.tasks_intro))),5000))
        assertTrue("Cemetery surface leaked into Tasks",scenes().isEmpty())
        awaitNativeFrame()
        val screenshot=File(context.getExternalFilesDir(null),"$name.png");device.takeScreenshot(screenshot)
        val bitmap=BitmapFactory.decodeFile(screenshot.path);var sky=0
        for(y in 0 until bitmap.height step 8)for(x in 0 until bitmap.width step 8){val p=bitmap.getPixel(x,y);val r=android.graphics.Color.red(p);val g=android.graphics.Color.green(p);val b=android.graphics.Color.blue(p);if(b>g+15 && g>r+15 && b>140)sky++}
        bitmap.recycle();assertEquals("A stale sky surface is visible over Tasks",0,sky)
    }
    private fun assertReadyPixels(){
        var bounds=android.graphics.Rect()
        onScene{s->val o=IntArray(2);s.getLocationOnScreen(o);bounds=android.graphics.Rect(o[0],o[1],o[0]+s.width,o[1]+s.height)}
        val file=File(context.cacheDir,"rendered-frame.png");device.takeScreenshot(file)
        val bitmap=BitmapFactory.decodeFile(file.path)
        var blue=0;var green=0;var black=0;var total=0
        for(y in bounds.top until bounds.bottom step 12)for(x in bounds.left until bounds.right step 12){
            val p=bitmap.getPixel(x,y);val r=android.graphics.Color.red(p);val g=android.graphics.Color.green(p);val b=android.graphics.Color.blue(p)
            if(b>g+15 && g>r+15 && b>140)blue++
            if(g>r*1.1f && g>b*1.3f)green++
            if(r<10 && g<10 && b<10)black++
            total++
        }
        bitmap.recycle();file.delete()
        assertTrue("Ready viewport has no sky",blue>total/20)
        assertTrue("Ready viewport is an empty backdrop, with no forest/grass",green>total/10)
        assertTrue("Ready viewport contains a black/blank surface",black<total/20)
    }
    @Test fun firstFrameIsRendered(){launch();assertReadyPixels();capture("ready-frame")}
    @Test fun repeatedTabsKeepOneEngineAndCameraRelativeClouds()=runBlocking<Unit>{
        val repo=deps.repository();repeat(48){repo.save("visual-fix-world-$it","World promise $it","",System.currentTimeMillis()-60_000)}
        val originalGraves=repo.snapshot.first().graves
        launch();assertReadyPixels()
        var engine:com.google.android.filament.Engine?=null
        var session:CemeteryRenderSession?=null
        onScene{engine=it.engine;session=(it.tag as CemeteryViewportState).session}
        val clouds=cloudPoints();val initial=frame()
        repeat(8){pan(true)};SystemClock.sleep(800);val left=frame();assertTrue(kotlin.math.abs(initial[0]-left[0])>5f);assertArrayEquals(clouds,cloudPoints(),.002f);capture("clouds-far-right")
        repeat(16){pan(false)};SystemClock.sleep(800);val right=frame();assertTrue(kotlin.math.abs(left[0]-right[0])>10f);assertArrayEquals(clouds,cloudPoints(),.002f);capture("clouds-far-left")
        pinch(1.7f);SystemClock.sleep(800);val zoom=frame();assertTrue(zoom[6]>right[6]);assertArrayEquals(clouds,cloudPoints(),.002f)
        pinch(.4f);SystemClock.sleep(800);assertTrue(frame()[6]<zoom[6]);assertArrayEquals(clouds,cloudPoints(),.002f);capture("clouds-wide")
        val retained=frame()
        repeat(20){cycle->
            if(cycle==10)deps.preferences().text("theme","dark")
            var previous:SceneView?=null;onScene{previous=it}
            click("Tasks");assertTasksHaveNoSurface("tasks-cycle-$cycle")
            instrumentation.runOnMainSync{assertFalse("Old viewport is still attached",previous!!.isAttachedToWindow);assertNull(previous!!.lifecycle);assertFalse(previous!!.uiHelper.isReadyToRender);assertNull(previous!!.onFrame)}
            if(cycle==0 || cycle==10)capture("tasks-no-surface-$cycle")
            click("Cemetery");ready();onScene{
                assertSame("Filament engine recreated on a tab switch",engine,it.engine)
                val retainedState=it.tag as CemeteryViewportState
                assertSame("GPU session recreated on a tab switch",session,retainedState.session)
                assertSame(session!!.renderer,it.renderer);assertSame(session!!.view,it.view)
                assertSame(session!!.scene,it.scene);assertTrue(retainedState.gate.ready)
                assertEquals("Readback kept running after readiness",1,retainedState.gate.probes)
            }
            assertReadyPixels()
            assertArrayEquals("Tab switch reset the camera",retained,frame(),.003f)
            assertArrayEquals("Cloud layer jumped after navigation",clouds,cloudPoints(),.002f)
        }
        val tabs=device.findObjects(By.text("Tasks")).last().visibleBounds;val cemetery=device.findObjects(By.text("Cemetery")).last().visibleBounds
        repeat(12){device.click(tabs.centerX(),tabs.centerY());device.click(cemetery.centerX(),cemetery.centerY())}
        click("Tasks");assertTasksHaveNoSurface("rapid-tabs");capture("rapid-tabs-tasks")
        click("Cemetery");ready();onScene{assertSame(engine,it.engine)};assertArrayEquals(clouds,cloudPoints(),.002f)
        assertEquals(originalGraves,repo.snapshot.first().graves)
        // About uses the installed Gradle version, rather than a stale localized literal.
        val settings=device.wait(Until.findObject(By.desc("Settings")),5000);settings.click();device.waitForIdle()
        repeat(3){device.swipe(device.displayWidth/2,device.displayHeight*3/4,device.displayWidth/2,device.displayHeight/3,25)}
        val installed=context.packageManager.getPackageInfo(context.packageName,0).versionName
        assertEquals(BuildConfig.VERSION_NAME,installed)
        assertTrue(device.wait(Until.hasObject(By.text(context.getString(R.string.about_body,installed))),5000));capture("about-version")
    }
    @After fun cleanup()=runBlocking<Unit>{
        capture("last-visible")
        device.pressHome()
        device.executeShellCommand("settings put global animator_duration_scale $originalMotion")
        deps.repository().snapshot.first().tasks.filter{it.id.startsWith("visual-fix-")}.forEach{deps.repository().delete(it.id)}
        original?.let{deps.preferences().text("language",it.language);deps.preferences().text("theme",it.theme)}
    }
}
