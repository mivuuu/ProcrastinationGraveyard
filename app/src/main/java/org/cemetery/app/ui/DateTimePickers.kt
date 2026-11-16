package org.cemetery.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.cemetery.app.R
import org.cemetery.app.core.*
import java.time.*
import java.time.format.*
import java.time.temporal.WeekFields
import java.text.NumberFormat
import java.text.DateFormatSymbols

internal fun calendarCells(month:YearMonth,first:DayOfWeek):List<LocalDate?> {
    val offset=(month.atDay(1).dayOfWeek.value-first.value+7)%7
    return (0 until ((offset+month.lengthOfMonth()+6)/7)*7).map{i->if(i>=offset && i<offset+month.lengthOfMonth())month.atDay(i-offset+1) else null}
}
@Composable fun CemeteryDateDialog(initial:LocalDate,close:()->Unit,choose:(LocalDate)->Unit){
    val locale=LocalContext.current.resources.configuration.locales[0];val c=CemeteryDesign.colors
    var epoch by rememberSaveable{mutableLongStateOf(initial.toEpochDay())}
    var monthOffset by rememberSaveable{mutableLongStateOf(0)}
    val month=YearMonth.from(initial).plusMonths(monthOffset);val first=WeekFields.of(locale).firstDayOfWeek
    val number=remember(locale){NumberFormat.getIntegerInstance(locale)}
    CemeteryDialog(close){
        val exit=LocalOverlayExit.current
        CemeteryText(stringResource(R.string.choose_date),style=CemeteryDesign.type.title)
        Row(verticalAlignment=Alignment.CenterVertically){
            CemeteryIconButton(CemeteryGlyph.BACK,stringResource(R.string.previous_month),{monthOffset--},soft=false)
            CemeteryText(month.format(DateTimeFormatter.ofPattern("LLLL yyyy",locale)),Modifier.weight(1f),style=CemeteryDesign.type.label,align=TextAlign.Center)
            CemeteryIconButton(CemeteryGlyph.CHEVRON,stringResource(R.string.next_month),{monthOffset++},soft=false)
        }
        Row{repeat(7){i->CemeteryText(first.plus(i.toLong()).getDisplayName(java.time.format.TextStyle.SHORT_STANDALONE,locale),Modifier.weight(1f),style=CemeteryDesign.type.small,align=TextAlign.Center)}}
        calendarCells(month,first).chunked(7).forEach{week->Row{
            week.forEach{date->
                val selected=date?.toEpochDay()==epoch
                Box(Modifier.weight(1f).heightIn(min=48.dp).clip(CemeteryShapes.pill).background(if(selected)c.accent else androidx.compose.ui.graphics.Color.Transparent)
                    .then(if(date!=null)Modifier.semantics{contentDescription=date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))}.selectable(selected,role=Role.RadioButton,onClick={epoch=date.toEpochDay()}) else Modifier),contentAlignment=Alignment.Center){
                    if(date!=null)CemeteryText(number.format(date.dayOfMonth),Modifier.padding(vertical=8.dp),style=CemeteryDesign.type.label,color=if(selected)c.onAccent else c.text)
                }
            }
        }}
        CemeteryButton(stringResource(R.string.save),{exit{choose(LocalDate.ofEpochDay(epoch))}},Modifier.fillMaxWidth())
        CemeteryDismissButton(stringResource(R.string.cancel),close,Modifier.fillMaxWidth(),kind=ButtonKind.QUIET)
    }
}
@Composable fun CemeteryTimeDialog(initial:LocalTime,close:()->Unit,choose:(LocalTime)->Unit){
    val context=LocalContext.current;val locale=context.resources.configuration.locales[0]
    val is24=android.text.format.DateFormat.is24HourFormat(context)
    var hour by rememberSaveable{mutableStateOf((if(is24)initial.hour else (initial.hour+11)%12+1).toString().padStart(2,'0'))}
    var minute by rememberSaveable{mutableStateOf(initial.minute.toString().padStart(2,'0'))}
    var period by rememberSaveable{mutableIntStateOf(if(initial.hour>=12)1 else 0)}
    val h=hour.toIntOrNull();val m=minute.toIntOrNull();val valid=h!=null && h in (if(is24)0..23 else 1..12) && m!=null && m in 0..59
    CemeteryDialog(close){
        val exit=LocalOverlayExit.current
        CemeteryText(stringResource(R.string.choose_time),style=CemeteryDesign.type.title)
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
            CemeteryTextField(hour,{hour=it.filter(Char::isDigit).take(2)},stringResource(R.string.hour),Modifier.weight(1f),keyboard=KeyboardOptions(keyboardType=KeyboardType.Number))
            CemeteryTextField(minute,{minute=it.filter(Char::isDigit).take(2)},stringResource(R.string.minute),Modifier.weight(1f),keyboard=KeyboardOptions(keyboardType=KeyboardType.Number))
        }
        if(!is24)Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){DateFormatSymbols(locale).amPmStrings.forEachIndexed{i,label->CemeteryFilterTab(label,period==i,{period=i},Modifier.weight(1f))}}
        if(!valid)CemeteryText(stringResource(R.string.validation_time),color=CemeteryDesign.colors.error,style=CemeteryDesign.type.small)
        CemeteryButton(stringResource(R.string.save),{if(valid)exit{choose(LocalTime.of(if(is24)h!! else h!!%12+period*12,m!!))}},Modifier.fillMaxWidth(),enabled=valid)
        CemeteryDismissButton(stringResource(R.string.cancel),close,Modifier.fillMaxWidth(),kind=ButtonKind.QUIET)
    }
}
