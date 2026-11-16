package org.cemetery.app.feature.cemetery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.cemetery.app.R
import org.cemetery.app.domain.*
import org.cemetery.app.core.*
import org.cemetery.app.ui.*

@Composable fun GraveSheet(grave: Grave, task: Task, history: List<History>, now: Long, close:()->Unit, complete:()->Unit, edit:()->Unit, remove:()->Unit) {
    val context=LocalContext.current
    var menu by remember { mutableStateOf(false) }
    CemeteryBottomSheet(onDismiss=close,tag="grave-sheet",fraction=gravePanelFraction()) {
        Column(Modifier.fillMaxWidth().weight(1f).clipToBounds().verticalScroll(rememberScrollState()).padding(start=24.dp,end=24.dp,bottom=16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Row {Column(Modifier.weight(1f)){
                CemeteryText(stringResource(R.string.grave_details),style=CemeteryDesign.type.small,color=CemeteryDesign.colors.secondary)
                CemeteryText(task.title,style=CemeteryDesign.type.title)
            };CemeteryIconButton(CemeteryGlyph.MORE,stringResource(R.string.delete),{menu=true})}
            if(task.description.isNotBlank())CemeteryText(task.description)
            CemeteryText(stringResource(R.string.status)+": "+stringResource(statusResource(task.status(now))),style=CemeteryDesign.type.label)
            CemeteryText(stringResource(R.string.original_deadline)+": "+dateTime(context,task.originalDeadline))
            CemeteryText(stringResource(R.string.missed_deadline)+": "+dateTime(context,grave.missedDeadline))
            if(grave.missedDeadline!=task.deadline)CemeteryText(stringResource(R.string.historical_grave),style=CemeteryDesign.type.small,color=CemeteryDesign.colors.secondary)
            CemeteryText(stringResource(R.string.deadline)+": "+dateTime(context,task.deadline))
            CemeteryText(stringResource(R.string.overdue_duration)+": "+duration(context,(task.completedAt?:now)-grave.missedDeadline))
            val array=when(grave.style){"philosophical"->R.array.epitaph_philosophical;"dark_humor"->R.array.epitaph_dark_humor;else->R.array.epitaph_neutral}
            CemeteryPanel(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                CemeteryText(stringResource(R.string.epitaph),style=CemeteryDesign.type.small,color=CemeteryDesign.colors.secondary)
                CemeteryText(context.resources.getStringArray(array)[grave.epitaphId].replace("{task_title}",task.title),style=CemeteryDesign.type.label)
            } }
            if(history.isNotEmpty()){
                CemeteryDivider();CemeteryText(stringResource(R.string.history),style=CemeteryDesign.type.label)
                history.forEach {CemeteryText(if(it.type=="rescheduled")stringResource(R.string.history_rescheduled,dateTime(context,it.previousDeadline),dateTime(context,it.newDeadline)) else stringResource(R.string.history_completed,dateTime(context,it.at)),style=CemeteryDesign.type.small,color=CemeteryDesign.colors.secondary)}
            }
            if(task.completedAt==null){CemeteryButton(stringResource(R.string.done),complete,Modifier.fillMaxWidth(),glyph=CemeteryGlyph.CHECK);CemeteryButton(stringResource(R.string.reschedule),edit,Modifier.fillMaxWidth(),kind=ButtonKind.SECONDARY)}
        }
        Column(Modifier.fillMaxWidth().padding(horizontal=24.dp).padding(top=8.dp,bottom=16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            CemeteryDivider()
            CemeteryButton(stringResource(R.string.close),close,Modifier.fillMaxWidth().testTag("grave-close"),kind=ButtonKind.QUIET)
        }
    }
    if(menu)CemeteryDialog({menu=false}){
        CemeteryText(stringResource(R.string.grave_details),style=CemeteryDesign.type.title)
        CemeteryButton(stringResource(R.string.delete),{menu=false;remove()},Modifier.fillMaxWidth(),kind=ButtonKind.DANGER,glyph=CemeteryGlyph.DELETE)
        CemeteryButton(stringResource(R.string.close),{menu=false},Modifier.fillMaxWidth(),kind=ButtonKind.QUIET)
    }
}
