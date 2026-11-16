package org.cemetery.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import org.cemetery.app.core.*

/** For native content only. SurfaceView destinations deliberately do not use this wrapper. */
@Composable internal fun CemeteryContentReveal(content:@Composable ()->Unit){
    var shown by remember{mutableStateOf(false)}
    LaunchedEffect(Unit){shown=true}
    val progress by animateFloatAsState(if(shown)1f else 0f,CemeteryMotion.spec(CemeteryMotion.SCREEN),label="content-reveal")
    Box(Modifier.graphicsLayer{alpha=progress;translationY=(1-progress)*6.dp.toPx()}){content()}
}
