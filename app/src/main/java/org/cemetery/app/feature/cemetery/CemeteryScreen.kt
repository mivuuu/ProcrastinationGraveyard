package org.cemetery.app.feature.cemetery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import org.cemetery.app.core.CemeteryMotion
import androidx.compose.ui.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clipToBounds
import org.cemetery.app.R
import org.cemetery.app.domain.*
import org.cemetery.app.core.CemeteryDesign
import org.cemetery.app.ui.*

@Composable
internal fun CemeteryScreen(store:CemeteryRenderStore,snapshot: Snapshot,now:Long,dark:Boolean,camera:CameraController,selectedId:String?,busy:Boolean,
                   onSelect:(Grave)->Unit,onPresentationState:(String?,Boolean)->Unit) {
    var ready by remember{mutableStateOf(false)}
    val cover by animateFloatAsState(if(ready)0f else 1f,CemeteryMotion.spec(CemeteryMotion.SCREEN),label="garden-reveal")
    var records by remember{mutableStateOf(false)}
    @Composable fun recordButton(){CemeteryIconButton(CemeteryGlyph.LIST,stringResource(R.string.grave_list),{records=true},enabled=!busy)}
    BoxWithConstraints(Modifier.fillMaxSize()){
        val compact=maxHeight<300.dp || LocalDensity.current.fontScale>=1.7f
        Column(Modifier.fillMaxSize()){
            if(!compact)Row(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f).padding(end=8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                    CemeteryText(stringResource(R.string.cemetery_intro),style=CemeteryDesign.type.label)
                    CemeteryText(stringResource(R.string.cemetery_gestures),style=CemeteryDesign.type.small,color=CemeteryDesign.colors.secondary)
                };recordButton()
            }
            Box(Modifier.fillMaxWidth().weight(1f).background(Color(CemeterySkyArgb))){
                CemeteryScene(Modifier.fillMaxSize().clipToBounds(),store,snapshot,now,dark,camera,selectedId,onSelect,onReady={ready=it},onPresentationState=onPresentationState)
                // Fade only the lightweight native readiness panel; the texture presents real frames.
                if(!ready || cover>0f)CemeteryPanel(Modifier.align(Alignment.Center).padding(24.dp).graphicsLayer{alpha=cover}){
                    Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp),horizontalAlignment=Alignment.CenterHorizontally){
                        CemeteryText(stringResource(R.string.preparing_garden))
                    }
                }
                if(compact)Box(Modifier.align(Alignment.TopEnd).padding(8.dp)){recordButton()}
                if(snapshot.graves.isEmpty() && ready)CemeteryPanel(Modifier.align(Alignment.BottomCenter).padding(20.dp),color=CemeteryDesign.colors.elevated.copy(alpha=.96f)){
                    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                        CemeteryText(stringResource(R.string.cemetery_empty),style=CemeteryDesign.type.label)
                        CemeteryText(stringResource(R.string.cemetery_empty_body),style=CemeteryDesign.type.small)
                    }
                }
            }
        }
    }
    if(records)CemeteryBottomSheet(onDismiss={records=false},fraction=.65f){
        val exit=LocalOverlayExit.current
        CemeteryText(stringResource(R.string.grave_count,snapshot.graves.size),Modifier.padding(horizontal=24.dp,vertical=8.dp),style=CemeteryDesign.type.title)
        LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            items(snapshot.graves,key={it.id}){grave->CemeteryButton(snapshot.tasks.find{it.id==grave.taskId}?.title.orEmpty(),{exit{records=false;onSelect(grave)}},Modifier.fillMaxWidth(),kind=ButtonKind.SECONDARY,glyph=CemeteryGlyph.GRAVE)}
        }
    }
}
