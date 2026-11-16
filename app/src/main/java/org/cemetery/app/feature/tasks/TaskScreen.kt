package org.cemetery.app.feature.tasks

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.cemetery.app.R
import org.cemetery.app.core.*
import org.cemetery.app.domain.*
import org.cemetery.app.ui.*

@Composable fun TaskScreen(snapshot:Snapshot,now:Long,edit:(Task?)->Unit,complete:(Task)->Unit,visit:(Grave)->Unit,remove:(Task)->Unit){
    var filter by rememberSaveable{mutableIntStateOf(0)}
    val tasks=snapshot.tasks.filter{when(filter){0->it.completedAt==null && it.deadline>=now;1->it.completedAt==null && it.deadline<now;else->it.completedAt!=null}}
    val c=CemeteryDesign.colors
    @Composable fun introduction(){
        Box(Modifier.fillMaxWidth().padding(bottom=22.dp)){
            CemeteryText(stringResource(R.string.tasks_intro),Modifier.fillMaxWidth().padding(end=12.dp),style=CemeteryDesign.type.heading)
            CemeterySpark(Modifier.align(Alignment.TopEnd).offset(y=(-9).dp).size(28.dp))
        }
        BoxWithConstraints(Modifier.fillMaxWidth()){
            val fit=maxWidth>=330.dp && LocalDensity.current.fontScale<=1.2f
            Row(Modifier.fillMaxWidth().then(if(fit)Modifier else Modifier.horizontalScroll(rememberScrollState())).padding(bottom=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                listOf(R.string.active,R.string.overdue,R.string.completed).forEachIndexed{i,label->CemeteryFilterTab(stringResource(label),filter==i,{filter=i},if(fit)Modifier.weight(1f) else Modifier,compact=fit)}
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()){
        val illustrationHeight=(maxHeight*.30f).coerceIn(145.dp,205.dp)
        val spacious=maxHeight>530.dp && LocalDensity.current.fontScale<1.5f
        Column(Modifier.fillMaxSize().padding(horizontal=24.dp)){
            if(tasks.isEmpty() && spacious){
                Spacer(Modifier.height(12.dp));introduction()
                Spacer(Modifier.weight(.6f))
                SproutIllustration(Modifier.fillMaxWidth().height(illustrationHeight))
                CemeteryText(stringResource(if(filter==0)R.string.empty_tasks else R.string.empty_history),Modifier.fillMaxWidth(),style=CemeteryDesign.type.title,align=TextAlign.Center)
                if(filter==0)CemeteryText(stringResource(R.string.empty_tasks_body),Modifier.fillMaxWidth().padding(top=10.dp,start=12.dp,end=12.dp),color=c.secondary,align=TextAlign.Center)
                Spacer(Modifier.weight(1f))
            }else{
                LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(top=12.dp,bottom=20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
                    item{Column{introduction()}}
                    if(tasks.isEmpty())item{
                        Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){
                            SproutIllustration(Modifier.fillMaxWidth().height(160.dp))
                            CemeteryText(stringResource(if(filter==0)R.string.empty_tasks else R.string.empty_history),style=CemeteryDesign.type.title,align=TextAlign.Center)
                            if(filter==0)CemeteryText(stringResource(R.string.empty_tasks_body),Modifier.padding(top=10.dp),color=c.secondary,align=TextAlign.Center)
                        }
                    }
                    items(tasks,key={it.id}){task->CemeteryTaskCard(task,now,{edit(task)},{complete(task)},snapshot.graves.lastOrNull{it.taskId==task.id}?.let{g->{visit(g)}},{remove(task)})}
                }
            }
            CemeteryButton(stringResource(R.string.new_task),{edit(null)},Modifier.fillMaxWidth().padding(top=16.dp,bottom=20.dp),glyph=CemeteryGlyph.PLUS)
        }
    }
}
@Composable private fun CemeteryTaskCard(task:Task,now:Long,edit:()->Unit,complete:()->Unit,visit:(()->Unit)?,remove:()->Unit){
    val c=CemeteryDesign.colors;val context=LocalContext.current;val status=task.status(now)
    CemeteryPanel(Modifier.fillMaxWidth().clickable(onClick=edit),color=if(status==TaskStatus.OVERDUE)c.elevated else c.surface){
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f)){
                    CemeteryText(task.title,style=CemeteryDesign.type.button)
                    CemeteryText(stringResource(statusResource(status)),style=CemeteryDesign.type.small,color=c.secondary)
                }
                if(task.completedAt==null)CemeteryCheckbox(false,complete,stringResource(R.string.done)) else CemeteryIcon(CemeteryGlyph.TASKS,color=c.secondary)
            }
            if(task.description.isNotBlank())CemeteryText(task.description,maxLines=3)
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                CemeteryIcon(CemeteryGlyph.CLOCK,color=c.secondary,modifier=Modifier.size(18.dp));CemeteryText(dateTime(context,task.deadline),style=CemeteryDesign.type.small,color=c.secondary)
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                if(visit!=null)CemeteryButton(stringResource(R.string.open_grave),visit,Modifier.weight(1f),kind=ButtonKind.QUIET) else Spacer(Modifier.weight(1f))
                if(visit!=null)Spacer(Modifier.width(8.dp))
                CemeteryIconButton(CemeteryGlyph.DELETE,stringResource(R.string.delete),remove,soft=false)
            }
        }
    }
}
