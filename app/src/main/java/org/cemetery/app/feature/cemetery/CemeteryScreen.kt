package org.cemetery.app.feature.cemetery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.cemetery.app.R
import org.cemetery.app.domain.*
import org.cemetery.app.core.CemeteryDesign
import org.cemetery.app.ui.*

@Composable
fun CemeteryScreen(snapshot: Snapshot,now:Long,dark:Boolean,camera:CameraController,selectedId:String?,busy:Boolean,
                   onSelect:(Grave)->Unit,onPresentationState:(String?,Boolean)->Unit) {
    var ready by remember{mutableStateOf(false)}
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
            Box(Modifier.fillMaxWidth().weight(1f)){
                CemeteryScene(Modifier.fillMaxSize(),snapshot,now,dark,camera,selectedId,onSelect,onReady={ready=true},onPresentationState=onPresentationState)
                if(!ready)CemeteryPanel(Modifier.fillMaxSize()){
                    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){
                        CemeteryLoading();Spacer(Modifier.height(16.dp));CemeteryText(stringResource(R.string.preparing_garden))
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
        CemeteryText(stringResource(R.string.grave_count,snapshot.graves.size),Modifier.padding(horizontal=24.dp,vertical=8.dp),style=CemeteryDesign.type.title)
        LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            items(snapshot.graves,key={it.id}){grave->CemeteryButton(snapshot.tasks.find{it.id==grave.taskId}?.title.orEmpty(),{records=false;onSelect(grave)},Modifier.fillMaxWidth(),kind=ButtonKind.SECONDARY,glyph=CemeteryGlyph.GRAVE)}
        }
    }
}
