package org.cemetery.app.ui

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import org.cemetery.app.core.*

internal val LocalOverlayExit=staticCompositionLocalOf<(()->Unit)->Unit>{{it()}}
internal val LocalOverlayClosing=staticCompositionLocalOf{false}

/** Retain the real dialog during its exit; run only its UI dismissal after the transition ends. */
internal class OverlayMotionState(val visibility:MutableTransitionState<Boolean>,var animated:Boolean){
    var afterExit by mutableStateOf<(() -> Unit)?>(null)
    var closing by mutableStateOf(false);private set
    fun exit(action:()->Unit){
        if(closing)return
        closing=true
        if(!animated){action();return}
        afterExit=action;visibility.targetState=false
    }
}
@Composable internal fun rememberOverlayMotion():Pair<OverlayMotionState,State<Float>>{
    val motion=CemeteryDesign.motion
    val state=remember{OverlayMotionState(MutableTransitionState(!motion).apply{targetState=true},motion)}
    SideEffect{state.animated=motion}
    val transition=updateTransition(state.visibility,label="overlay")
    val opacity=transition.animateFloat(transitionSpec={CemeteryMotion.spec(CemeteryMotion.PANEL)},label="panel-opacity"){if(it)1f else 0f}
    LaunchedEffect(state.visibility.currentState,state.visibility.isIdle,state.afterExit){
        if(state.closing && state.visibility.isIdle && !state.visibility.currentState){val action=state.afterExit;state.afterExit=null;action?.invoke()}
    }
    return state to opacity
}
@Composable internal fun OverlayMotionContent(state:OverlayMotionState,content:@Composable ()->Unit){
    CompositionLocalProvider(LocalOverlayExit provides state::exit,LocalOverlayClosing provides state.closing,content=content)
}
@Composable fun CemeteryDismissButton(text:String,action:()->Unit,modifier:Modifier=Modifier,kind:ButtonKind=ButtonKind.QUIET){
    val exit=LocalOverlayExit.current
    CemeteryButton(text,{exit(action)},modifier,kind=kind)
}
