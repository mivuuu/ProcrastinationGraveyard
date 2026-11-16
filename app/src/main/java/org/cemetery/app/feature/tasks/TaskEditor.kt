package org.cemetery.app.feature.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.cemetery.app.R
import org.cemetery.app.core.*
import org.cemetery.app.domain.*
import org.cemetery.app.ui.*
import java.time.*

@Composable fun TaskEditor(task:Task?,close:()->Unit,save:(String?,String,String,Long)->Unit){
    var title by rememberSaveable(task?.id){mutableStateOf(task?.title?:"")}
    var description by rememberSaveable(task?.id){mutableStateOf(task?.description?:"")}
    var deadline by rememberSaveable(task?.id){mutableLongStateOf(task?.deadline?:System.currentTimeMillis()+86_400_000)}
    var picker by rememberSaveable{mutableIntStateOf(0)}
    var invalid by remember{mutableStateOf(false)}
    val context=LocalContext.current;val local=Instant.ofEpochMilli(deadline).atZone(ZoneId.systemDefault())
    when(picker){
        1->CemeteryDateDialog(local.toLocalDate(),{picker=0}){date->deadline=date.atTime(local.toLocalTime()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();picker=0}
        2->CemeteryTimeDialog(local.toLocalTime(),{picker=0}){time->deadline=local.toLocalDate().atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();picker=0}
        else->CemeteryDialog(close){
            CemeteryText(stringResource(if(task==null)R.string.add else R.string.edit),style=CemeteryDesign.type.title)
            CemeteryTextField(title,{title=it;invalid=false},stringResource(R.string.title),error=if(invalid)stringResource(R.string.validation_title) else null)
            CemeteryTextField(description,{description=it},stringResource(R.string.description),singleLine=false)
            CemeteryText(stringResource(R.string.deadline),style=CemeteryDesign.type.label)
            CemeteryText(dateTime(context,deadline),color=CemeteryDesign.colors.secondary)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                CemeteryButton(stringResource(R.string.choose_date),{picker=1},Modifier.weight(1f),kind=ButtonKind.SECONDARY,glyph=CemeteryGlyph.CALENDAR)
                CemeteryButton(stringResource(R.string.choose_time),{picker=2},Modifier.weight(1f),kind=ButtonKind.SECONDARY,glyph=CemeteryGlyph.CLOCK)
            }
            CemeteryText(stringResource(R.string.time_zone,ZoneId.systemDefault().id),style=CemeteryDesign.type.small,color=CemeteryDesign.colors.secondary)
            CemeteryButton(stringResource(R.string.save),{if(LifecycleRules.validate(title)){save(task?.id,title,description,deadline);close()}else invalid=true},Modifier.fillMaxWidth())
            CemeteryButton(stringResource(R.string.cancel),close,Modifier.fillMaxWidth(),kind=ButtonKind.QUIET)
        }
    }
}
