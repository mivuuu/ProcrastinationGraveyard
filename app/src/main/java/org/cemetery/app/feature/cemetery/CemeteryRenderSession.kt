package org.cemetery.app.feature.cemetery

import android.content.Context
import android.view.View
import com.google.android.filament.*
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import androidx.compose.ui.graphics.Color
import kotlin.math.pow

internal val CemeterySkyArgb=0xFF8CC6FB.toInt()

/** App-composition lifetime. No frame loop or Android surface exists while the tab is hidden. */
internal class CemeteryRenderStore(private val engine:Engine,private val context:Context):AutoCloseable {
    private var current:CemeteryRenderSession?=null
    fun session(camera:CameraController)=current?:CemeteryRenderSession(engine,context.applicationContext,camera).also{current=it}
    override fun close(){current?.close();current=null}
}

/** Expensive Filament resources and world geometry survive ordinary destination changes. */
internal class CemeteryRenderSession(val engine:Engine,context:Context,camera:CameraController):AutoCloseable {
    val loader=SceneView.createMaterialLoader(engine,context)
    val modelLoader=SceneView.createModelLoader(engine,context)
    val environmentLoader=SceneView.createEnvironmentLoader(engine,context)
    val meshes=CartoonMeshFactory(engine)
    private val stone=loader.createMaterial("materials/garden.filamat")
    private val flat=loader.createMaterial("materials/outline.filamat")
    val palette=run {
        val colors=listOf(0xFF91C94B,0xFF95CC50,0xFFBEC2C8,0xFFC8CBD0,0xFFB8BEC6,0xFFCDD0D4,
            0xFFFFFEF3,0xFF448859,0xFF66A545,0xFF428E54,0xFFC6A3DE,0xFFF5CE5A,
            0xFF348765,0xFF54A35B,0xFF91C982,CemeterySkyArgb.toLong() and 0xffffffffL,0xFFA4D7FC,0xFF64AA46,
            0xFF969FA9,0xFFAF8552,0xFF8CC64A,0xFFDEEFFD).map{Color(it)}+
            (0..11).map{ i->androidx.compose.ui.graphics.lerp(Color(0xFFA0D4FC),Color(0xFF86C3FA),i/11f)}
        colors.mapIndexed{index,c->loader.createInstance(if(index in 2..5)stone else flat).apply{
            setParameter("baseColor",c.red.toDouble().pow(2.2).toFloat(),c.green.toDouble().pow(2.2).toFloat(),c.blue.toDouble().pow(2.2).toFloat(),1f)
        }}
    }
    val environment=SceneView.createEnvironment(engine,
        indirectLight=IndirectLight.Builder().irradiance(1,floatArrayOf(1f,1f,1f)).intensity(20_000f).build(engine),
        skybox=Skybox.Builder().color(.25f,.55f,.95f,1f).build(engine))
    val grading=ColorGrading.Builder().toneMapper(ToneMapper.Linear()).build(engine)
    val renderer=SceneView.createRenderer(engine)
    val scene=SceneView.createScene(engine)
    val view=SceneView.createView(engine).apply{
        val old=colorGrading;colorGrading=grading;old?.let{engine.destroyColorGrading(it)}
    }
    val cameraNode=SceneView.createCameraNode(engine).apply{
        position=Position(camera.x,CameraController.HEIGHT,CameraController.DEPTH)
        lookAt(Position(camera.x,0f,CameraController.DEPTH-CameraController.LOOK_DEPTH))
        focalLength=camera.focalLength.toDouble();near=.1f;far=220f
    }
    val light=SceneView.createMainLightNode(engine)
    val world=DioramaWorld(engine,meshes,palette)
    var viewport:CemeteryTextureView?=null;private set
    private var closed=false
    fun bind(viewport:CemeteryTextureView){this.viewport=viewport}
    fun detach(viewport:CemeteryTextureView){
        viewport.visibility=View.GONE;viewport.onFrame=null;viewport.onSceneTouch=null
        // Remove scene entities, collision registrations and node listeners before releasing the wrapper.
        viewport.childNodes=emptyList()
        if(this.viewport===viewport){cameraNode.collisionSystem=null;this.viewport=null}
    }
    override fun close(){
        if(closed)return;closed=true
        viewport?.let{detach(it);it.destroy()}
        view.camera=null
        world.close();meshes.close();cameraNode.destroy();light.destroy()
        view.colorGrading=null
        engine.destroyView(view);engine.destroyRenderer(renderer);engine.destroyScene(scene)
        engine.destroyColorGrading(grading)
        environment.indirectLight?.let{engine.destroyIndirectLight(it)}
        environment.skybox?.let{engine.destroySkybox(it)}
        modelLoader.destroy();environmentLoader.destroy();loader.destroy()
    }
}
