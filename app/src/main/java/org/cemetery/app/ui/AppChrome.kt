package org.cemetery.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.cemetery.app.R
import org.cemetery.app.core.*

@Composable fun CemeteryBackground(content:@Composable ()->Unit){
    val c=CemeteryDesign.colors
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(c.background,c.backgroundEnd)))){content()}
}
@Composable fun CemeteryHeader(title:String,settings:(()->Unit)?,back:(()->Unit)?=null){
    val compact=LocalConfiguration.current.screenHeightDp<480 || LocalDensity.current.fontScale>=1.7f
    Row(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=if(compact)6.dp else 12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
        if(back!=null)CemeteryIconButton(CemeteryGlyph.BACK,stringResource(R.string.close),back)
        CemeteryText(title,Modifier.weight(1f),style=if(compact)CemeteryDesign.type.title else CemeteryDesign.type.screen)
        settings?.let{CemeteryIconButton(CemeteryGlyph.SETTINGS,stringResource(R.string.settings),it,Modifier.size(48.dp))}
    }
}
@Composable fun CemeteryNavigationBar(route:String,tasks:()->Unit,cemetery:()->Unit){
    val c=CemeteryDesign.colors
    val wide=LocalConfiguration.current.screenHeightDp<480
    val large=LocalDensity.current.fontScale>=1.7f
    var last by remember{mutableIntStateOf(if(route=="cemetery")1 else 0)}
    SideEffect{if(route=="tasks" || route=="cemetery")last=if(route=="cemetery")1 else 0}
    val position by animateFloatAsState(if(route=="cemetery")1f else if(route=="tasks")0f else last.toFloat(),CemeteryMotion.spec(CemeteryMotion.NAVIGATION),label="navigation-position")
    val visibility by animateFloatAsState(if(route=="settings")0f else 1f,CemeteryMotion.spec(CemeteryMotion.SELECTION),label="navigation-visibility")
    val gap=if(large)8.dp else 20.dp
    Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp).clip(CemeteryShapes.pill).background(c.navigation).padding(horizontal=if(large)12.dp else 20.dp,vertical=10.dp)
        .drawBehind{
            val width=(size.width-gap.toPx())/2
            drawRoundRect(c.selected.copy(alpha=visibility),Offset((width+gap.toPx())*position,0f),Size(width,size.height),CornerRadius(28.dp.toPx()))
        },horizontalArrangement=Arrangement.spacedBy(gap)){
        listOf(Triple("tasks",R.string.tasks,CemeteryGlyph.TASKS),Triple("cemetery",R.string.cemetery,CemeteryGlyph.GRAVE)).forEach{(key,label,glyph)->
            val selected=key==route
            val source=remember{MutableInteractionSource()};val pressed by source.collectIsPressedAsState()
            val foreground by animateColorAsState(if(selected)c.text else c.secondary,CemeteryMotion.spec(CemeteryMotion.NAVIGATION),label="navigation-label")
            val scale by animateFloatAsState(if(pressed).97f else 1f,CemeteryMotion.spec(CemeteryMotion.PRESS),label="navigation-press")
            val tab=Modifier.weight(1f).selectable(selected,interactionSource=source,indication=null,role=Role.Tab,onClick=if(key=="tasks")tasks else cemetery)
                .heightIn(min=48.dp).graphicsLayer{scaleX=scale;scaleY=scale}.padding(horizontal=if(large)8.dp else 12.dp,vertical=7.dp)
            @Composable fun labelContent(){
                CemeteryIcon(glyph,color=foreground,modifier=Modifier.size(26.dp))
                CemeteryText(stringResource(label),style=CemeteryDesign.type.small.copy(fontWeight=androidx.compose.ui.text.font.FontWeight.ExtraBold,fontSize=if(large)12.sp else 14.sp),color=foreground,align=TextAlign.Center)
            }
            if(wide)Row(tab,verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp,Alignment.CenterHorizontally)){labelContent()}
            else Column(tab,horizontalAlignment=Alignment.CenterHorizontally){labelContent()}
        }
    }
}
