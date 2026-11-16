package org.cemetery.app.feature.cemetery

import android.animation.ValueAnimator
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.sceneview.*
import io.github.sceneview.math.Position
import org.cemetery.app.R
import org.cemetery.app.domain.*
import org.cemetery.app.ui.gravePanelFraction
import kotlin.math.*

private class CartoonFrame {
    var time=0L;var center=Int.MIN_VALUE;var radius=-1
    var x=Float.NaN;var focal=Float.NaN;var width=0;var height=0
    var travel=Float.NaN;var yaw=Float.NaN;var spec:StoneSpec?=null
    var reportedId:String?=null;var reportedReady=false
    var scene:CemeteryTextureView?=null;val origin=IntArray(2);var available=.47f
}

@Composable
internal fun CemeteryScene(modifier:Modifier,store:CemeteryRenderStore,snapshot:Snapshot,now:Long,dark:Boolean,camera:CameraController,
                  selectedId:String?,onSelect:(Grave)->Unit,onReady:(Boolean)->Unit,
                  onPresentationState:(String?,Boolean)->Unit) {
    val host=org.cemetery.app.core.LocalHostActivity.current!!
    camera.configure(snapshot.graves)
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    val ready by rememberUpdatedState(onReady)
    val gate=remember{SurfaceFrameGate{ready(it)}}
    DisposableEffect(gate){onDispose{gate.close()}}
    val session=remember(store){store.session(camera)}
    val cameraNode=session.cameraNode
    val presentation=remember{GravePresentation()}
    val frame=remember{CartoonFrame()}
    var heldId by remember{mutableStateOf<String?>(null)}
    var sections by remember{mutableStateOf(CemeteryPlacement.nearby(camera.x))}
    val specs=remember(snapshot.graves,sections,heldId){snapshot.graves.filter{it.chunk in sections || it.id==heldId}.map(::StoneSpec)}
    val selectedSpec=remember(snapshot.graves,heldId){snapshot.graves.find{it.id==heldId}?.let(::StoneSpec)}
    val liveSpec by rememberUpdatedState(selectedSpec)
    val plants=remember(snapshot.tasks,now/60_000,sections){if(0 in sections)snapshot.tasks.filter{it.completedAt==null && it.deadline>=now}.take(16).map{SproutSpec(it.id,(it.health(now)*10).toInt())} else emptyList()}
    val world=session.world
    val nodes=remember(world,sections,specs,plants,heldId){world.update(sections,specs,plants,heldId);world.nodes}
    val stateCallback by rememberUpdatedState(onPresentationState)
    val latestSections by rememberUpdatedState(sections)
    val latestSpecs by rememberUpdatedState(specs)
    val latestPlants by rememberUpdatedState(plants)
    DisposableEffect(world){onDispose{
        frame.scene?.let(session::detach);frame.scene=null
        presentation.id?.let(world::restore)
        presentation.reset();camera.unlock()
        world.update(latestSections,latestSpecs,latestPlants,null);world.releaseRetired()
        stateCallback(null,false)
    }}
    LaunchedEffect(selectedId,world){
        if(selectedId==null){
            val closing=presentation.id
            presentation.dismiss(ValueAnimator.areAnimatorsEnabled())
            if(closing!=null && !presentation.busy){world.restore(closing);heldId=null;camera.unlock()}
            stateCallback(presentation.id,false)
        }
        else if(presentation.select(selectedId,ValueAnimator.areAnimatorsEnabled())){
            frame.spec=null // Reapply the foreground pose even when reduced-motion travel/yaw repeat.
            camera.lockForPresentation();heldId=selectedId;stateCallback(selectedId,presentation.informationReady)
        }
    }
    LaunchedEffect(snapshot.graves,heldId){
        val id=heldId
        if(id!=null && snapshot.graves.none{it.id==id}){world.restore(id);presentation.reset();heldId=null;camera.unlock();stateCallback(null,false)}
    }
    val panelFraction=gravePanelFraction()
    DisposableEffect(lifecycle){
        val observer=LifecycleEventObserver{_,event->
            frame.time=0L
            gate.reset()
            frame.scene?.visibility=if(event==Lifecycle.Event.ON_RESUME)android.view.View.VISIBLE else android.view.View.INVISIBLE
        }
        lifecycle.addObserver(observer)
        onDispose{
            lifecycle.removeObserver(observer)
            frame.scene?.let(session::detach)
            frame.scene=null
        }
    }
    val context=LocalContext.current
    val touch=remember(camera,context){CemeteryGestures(camera,ViewConfiguration.get(context).scaledTouchSlop.toFloat(),context.resources.displayMetrics.density)}
    val select by rememberUpdatedState(onSelect)
    val currentGraves by rememberUpdatedState(snapshot.graves)
    val description=stringResource(R.string.cemetery_accessibility)
    CemeteryViewport(modifier=modifier.semantics{contentDescription=description},activity=host,lifecycle=lifecycle,
        session=session,childNodes=nodes,
        onViewCreated={
            frame.scene=this;session.bind(this);tag=CemeteryViewportState(session,gate)
            visibility=if(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))android.view.View.VISIBLE else android.view.View.INVISIBLE
        },
        onViewUpdated={world.releaseRetired()},
        onTouchEvent={event,hit->
            if(presentation.busy || !gate.ready){touch.cancel();true} else {
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
                if(frame.height>0)frame.available=((windowHeight*(1f-panelFraction-.08f)-frame.origin[1])/frame.height).coerceIn(.18f,.65f)
            }
            if(framingChanged || viewportChanged)world.frameSky(cameraNode)
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
            frame.scene?.let(gate::afterFrame)
        })
}
