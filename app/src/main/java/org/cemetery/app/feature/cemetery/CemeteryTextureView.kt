package org.cemetery.app.feature.cemetery

import android.content.Context
import android.graphics.SurfaceTexture
import android.view.MotionEvent
import android.view.TextureView
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import io.github.sceneview.SceneView
import io.github.sceneview.collision.HitResult
import io.github.sceneview.node.Node

/**
 * SceneView owns the scene, picking, projection, frame loop and destination lifecycle.
 * Filament UiHelper explicitly supports a TextureView target. Its ordinary window composition
 * avoids SurfaceView holes, z-order and alpha restrictions during native UI animations.
 * The controller's SurfaceView is never attached or rendered; only this TextureView is hosted.
 */
internal class CemeteryTextureView(context:Context,session:CemeteryRenderSession,activity:ComponentActivity,lifecycle:Lifecycle):TextureView(context){
    private val controller=object:SceneView(
        context=context,sharedEngine=session.engine,sharedModelLoader=session.modelLoader,
        sharedMaterialLoader=session.loader,sharedEnvironmentLoader=session.environmentLoader,
        sharedScene=session.scene,sharedView=session.view,sharedRenderer=session.renderer,
        sharedCameraNode=session.cameraNode,sharedMainLightNode=session.light,sharedEnvironment=session.environment,
        cameraManipulator=null,onGestureListener=null,sharedActivity=activity,sharedLifecycle=lifecycle
    ){
        override fun getDisplay()=this@CemeteryTextureView.display
    }
    private var completion:(()->Unit)?=null
    private var destroyed=false
    init {
        controller.uiHelper.attachTo(this)
        // An unproduced texture remains transparent over the blue viewport, never black.
        isOpaque=false
        val delegate=checkNotNull(surfaceTextureListener)
        surfaceTextureListener=object:SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface:SurfaceTexture,width:Int,height:Int){delegate.onSurfaceTextureAvailable(surface,width,height)}
            override fun onSurfaceTextureSizeChanged(surface:SurfaceTexture,width:Int,height:Int){delegate.onSurfaceTextureSizeChanged(surface,width,height)}
            override fun onSurfaceTextureDestroyed(surface:SurfaceTexture):Boolean{completion=null;return delegate.onSurfaceTextureDestroyed(surface)}
            override fun onSurfaceTextureUpdated(surface:SurfaceTexture){
                delegate.onSurfaceTextureUpdated(surface)
                if(!destroyed){val callback=completion;completion=null;callback?.invoke()}
            }
        }
    }
    val engine get()=controller.engine
    val renderer get()=controller.renderer
    val scene get()=controller.scene
    val view get()=controller.view
    val cameraNode get()=controller.cameraNode
    val uiHelper get()=controller.uiHelper
    val lifecycle get()=controller.lifecycle
    var childNodes:List<Node>
        get()=controller.childNodes
        set(value){controller.childNodes=value}
    var onFrame:((Long)->Unit)?
        get()=controller.onFrame
        set(value){controller.onFrame=value}
    var onSceneTouch:((MotionEvent,HitResult?)->Boolean)?
        get()=controller.onTouchEvent
        set(value){controller.onTouchEvent=value}
    override fun onTouchEvent(event:MotionEvent)=controller.onTouchEvent(event)
    fun whenFrameRendered(callback:()->Unit){completion=callback}
    fun destroy(){if(!destroyed){destroyed=true;completion=null;controller.destroy()}}
}
