package org.cemetery.app.feature.cemetery

import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import io.github.sceneview.collision.HitResult
import io.github.sceneview.node.Node

/** Each destination owns one texture/swapchain. GPU geometry survives in the render session. */
@Composable internal fun CemeteryViewport(
    modifier:Modifier,session:CemeteryRenderSession,activity:ComponentActivity,lifecycle:Lifecycle,
    childNodes:List<Node>,onTouchEvent:(MotionEvent,HitResult?)->Boolean,onFrame:(Long)->Unit,
    onViewCreated:CemeteryTextureView.()->Unit,onViewUpdated:CemeteryTextureView.()->Unit
){
    AndroidView(modifier=modifier,factory={context->
        session.viewport?.let{session.detach(it);it.destroy()}
        CemeteryTextureView(context,session,activity,lifecycle).also(onViewCreated)
    },update={viewport->
        viewport.childNodes=childNodes
        viewport.onSceneTouch=onTouchEvent;viewport.onFrame=onFrame
        onViewUpdated(viewport)
    },onReset=null,onRelease={viewport->session.detach(viewport);viewport.destroy()})
}
