package org.cemetery.app.feature.cemetery

import android.animation.ValueAnimator
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.lifecycle.*
import com.google.android.filament.ColorGrading
import com.google.android.filament.ToneMapper
import com.google.android.filament.IndirectLight
import com.google.android.filament.Skybox
import io.github.sceneview.*
import io.github.sceneview.math.Position
import org.cemetery.app.R
import org.cemetery.app.domain.*
import kotlin.math.*

private class CartoonFrame {
    var time=0L;var frames=0;var center=Int.MIN_VALUE;var radius=-1
    var x=Float.NaN;var focal=Float.NaN;var width=0;var height=0
    var travel=Float.NaN;var yaw=Float.NaN;var spec:StoneSpec?=null
    var reportedId:String?=null;var reportedReady=false
    var scene:SceneView?=null;val origin=IntArray(2);var available=.47f
}

@Composable
fun CemeteryScene(modifier:Modifier,snapshot:Snapshot,now:Long,dark:Boolean,camera:CameraController,
                  selectedId:String?,onSelect:(Grave)->Unit,onReady:()->Unit,
                  onPresentationState:(String?,Boolean)->Unit) {
    val host=org.cemetery.app.core.LocalHostActivity.current!!
    camera.configure(snapshot.graves)
    val engine=rememberEngine()
    val loader=rememberMaterialLoader(engine)
    val meshes=remember(engine){CartoonMeshFactory(engine)}
    DisposableEffect(meshes){onDispose{meshes.close()}}
    val environment=rememberEnvironment(engine,environment=remember(engine){{SceneView.createEnvironment(engine,
        indirectLight=IndirectLight.Builder().irradiance(1,floatArrayOf(1f,1f,1f)).intensity(20_000f).build(engine),
        skybox=Skybox.Builder().color(.25f,.55f,.95f,1f).build(engine))}})
    val stone=remember(loader){loader.createMaterial("materials/garden.filamat")}
    val flat=remember(loader){loader.createMaterial("materials/outline.filamat")}
    val palette=remember(loader){
        val colors=listOf(0xFF91C94B,0xFF95CC50,0xFFBEC2C8,0xFFC8CBD0,0xFFB8BEC6,0xFFCDD0D4,
            0xFFFFFEF3,0xFF448859,0xFF66A545,0xFF428E54,0xFFC6A3DE,0xFFF5CE5A,
            0xFF348765,0xFF54A35B,0xFF91C982,0xFF8CC6FB,0xFFA4D7FC,0xFF64AA46,
            0xFF969FA9,0xFFAF8552,0xFF8CC64A,0xFFDEEFFD).map{Color(it)}+
            (0..11).map{ i->androidx.compose.ui.graphics.lerp(Color(0xFFA0D4FC),Color(0xFF86C3FA),i/11f)}
        colors.mapIndexed{index,c->loader.createInstance(if(index in 2..5)stone else flat).apply{
            setParameter("baseColor",c.red.toDouble().pow(2.2).toFloat(),c.green.toDouble().pow(2.2).toFloat(),c.blue.toDouble().pow(2.2).toFloat(),1f)
        }}
    }
    DisposableEffect(palette){onDispose{palette.forEach{loader.destroyMaterialInstance(it)}}}
    val grading=remember(engine){ColorGrading.Builder().toneMapper(ToneMapper.Linear()).build(engine)}
    DisposableEffect(grading){onDispose{runCatching{engine.destroyColorGrading(grading)}}}
    val cameraNode=rememberCameraNode(engine){
        position=Position(camera.x,CameraController.HEIGHT,CameraController.DEPTH)
        lookAt(Position(camera.x,0f,CameraController.DEPTH-CameraController.LOOK_DEPTH))
        focalLength=camera.focalLength.toDouble();near=.1f;far=220f
    }
    val presentation=remember{GravePresentation()}
    var heldId by remember{mutableStateOf<String?>(null)}
    var sections by remember{mutableStateOf(CemeteryPlacement.nearby(camera.x))}
    val specs=remember(snapshot.graves,sections,heldId){snapshot.graves.filter{it.chunk in sections || it.id==heldId}.map(::StoneSpec)}
    val selectedSpec=remember(snapshot.graves,heldId){snapshot.graves.find{it.id==heldId}?.let(::StoneSpec)}
    val liveSpec by rememberUpdatedState(selectedSpec)
    val plants=remember(snapshot.tasks,now/60_000,sections){if(0 in sections)snapshot.tasks.filter{it.completedAt==null && it.deadline>=now}.take(16).map{SproutSpec(it.id,(it.health(now)*10).toInt())} else emptyList()}
    val world=remember(engine,palette){DioramaWorld(engine,meshes,palette).apply{update(sections,specs,plants,heldId)}}
    var nodes by remember(world){mutableStateOf(world.nodes)}
    val stateCallback by rememberUpdatedState(onPresentationState)
    DisposableEffect(world){onDispose{presentation.reset();camera.unlock();world.close();stateCallback(null,false)}}
    LaunchedEffect(world,sections,specs,plants,heldId){if(world.update(sections,specs,plants,heldId))nodes=world.nodes}
    LaunchedEffect(selectedId,world){
        if(selectedId==null){
            val closing=presentation.id
            presentation.dismiss(ValueAnimator.areAnimatorsEnabled())
            if(closing!=null && !presentation.busy){world.restore(closing);heldId=null;camera.unlock()}
            stateCallback(presentation.id,false)
        }
        else if(presentation.select(selectedId,ValueAnimator.areAnimatorsEnabled())){
            camera.lockForPresentation();heldId=selectedId;stateCallback(selectedId,presentation.informationReady)
        }
    }
    LaunchedEffect(snapshot.graves,heldId){
        val id=heldId
        if(id!=null && snapshot.graves.none{it.id==id}){world.restore(id);presentation.reset();heldId=null;camera.unlock();stateCallback(null,false)}
    }
    val frame=remember{CartoonFrame()}
    DisposableEffect(host){
        val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_STOP || event==Lifecycle.Event.ON_START)frame.time=0L}
        host.lifecycle.addObserver(observer);onDispose{host.lifecycle.removeObserver(observer)}
    }
    val context=LocalContext.current
    val touch=remember(camera,context){CemeteryGestures(camera,ViewConfiguration.get(context).scaledTouchSlop.toFloat(),context.resources.displayMetrics.density)}
    val select by rememberUpdatedState(onSelect)
    val currentGraves by rememberUpdatedState(snapshot.graves)
    val ready by rememberUpdatedState(onReady)
    val description=stringResource(R.string.cemetery_accessibility)
    Scene(modifier=modifier.semantics{contentDescription=description},activity=host,lifecycle=host.lifecycle,
        engine=engine,materialLoader=loader,environment=environment,cameraNode=cameraNode,childNodes=nodes,
        cameraManipulator=null,onGestureListener=null,
        onViewCreated={frame.scene=this;val old=view.colorGrading;view.colorGrading=grading;old?.let{runCatching{engine.destroyColorGrading(it)}}},
        onViewUpdated={world.releaseRetired()},
        onTouchEvent={event,hit->
            if(presentation.busy){touch.cancel();true} else {
                fun span()=hypot(event.getX(0)-event.getX(1),event.getY(0)-event.getY(1))
                when(event.actionMasked){
                    MotionEvent.ACTION_DOWN->touch.down(event.x,event.y)
                    MotionEvent.ACTION_POINTER_DOWN->if(event.pointerCount>=2)touch.secondPointer(span())
                    MotionEvent.ACTION_MOVE->if(event.pointerCount>=2)touch.pinch(span()) else touch.move(event.x,event.y)
                    MotionEvent.ACTION_POINTER_UP->touch.pointerUp()
                    MotionEvent.ACTION_UP->if(touch.up(event.x,event.y))world.hits[hit?.node]?.let{id->currentGraves.find{it.id==id}?.let(select)}
                    MotionEvent.ACTION_CANCEL->touch.cancel()
                };true
            }
        },
        onFrame={time->
            val dt=if(frame.time==0L)1f/60 else ((time-frame.time)/1_000_000_000f).coerceIn(0f,.25f)
            frame.time=time;camera.advance(dt)
            val viewport=cameraNode.viewport
            val aspect=if(viewport!=null && viewport.height>0)viewport.width.toFloat()/viewport.height else .65f
            val viewportChanged=frame.width!=viewport?.width || frame.height!=viewport?.height
            val framingChanged=abs(camera.x-frame.x)>.0005f || abs(camera.focalLength-frame.focal)>.002f || frame.x.isNaN()
            if(abs(camera.x-frame.x)>.0005f || frame.x.isNaN()){cameraNode.position=Position(camera.x,CameraController.HEIGHT,CameraController.DEPTH);frame.x=camera.x}
            if(abs(camera.focalLength-frame.focal)>.002f || frame.focal.isNaN()){cameraNode.focalLength=camera.focalLength.toDouble();frame.focal=camera.focalLength}
            if(viewportChanged){
                frame.width=viewport?.width?:0;frame.height=viewport?.height?:0
                frame.scene?.getLocationOnScreen(frame.origin)
                val windowHeight=host.window.decorView.height
                if(frame.height>0)frame.available=((windowHeight*.48f-frame.origin[1])/frame.height).coerceIn(.18f,.65f)
            }
            if(framingChanged || viewportChanged)world.frameSky(camera,aspect)
            val oldId=presentation.id
            presentation.advance(dt)
            if(oldId!=null && presentation.id==null){
                world.restore(oldId);presentation.reset();heldId=null;camera.unlock()
            }
            liveSpec?.let{g->if(presentation.busy && (presentation.travel!=frame.travel || presentation.yaw!=frame.yaw || g!=frame.spec || viewportChanged)){
                world.transform(g.id,PresentationFraming.pose(g.x,g.z,g.height,camera.x,CameraController.HEIGHT,CameraController.DEPTH,CameraController.LOOK_DEPTH,camera.focalLength,aspect,presentation.travel,presentation.yaw,frame.available))
                frame.travel=presentation.travel;frame.yaw=presentation.yaw;frame.spec=g
            }}
            if(frame.reportedId!=presentation.id || frame.reportedReady!=presentation.informationReady){frame.reportedId=presentation.id;frame.reportedReady=presentation.informationReady;stateCallback(presentation.id,presentation.informationReady)}
            val radius=(ceil(48f*12f/camera.focalLength*aspect/CemeteryPlacement.SECTION_WIDTH).toInt()+1).coerceIn(2,5)
            val center=CemeteryPlacement.sectionAt(camera.x)
            if(center!=frame.center || radius!=frame.radius){frame.center=center;frame.radius=radius;sections=center-radius..center+radius}
            if(++frame.frames==4)ready()
        })
}
