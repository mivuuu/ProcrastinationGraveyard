package org.cemetery.app.feature.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import org.cemetery.app.BuildConfig
import org.cemetery.app.R
import org.cemetery.app.data.Settings
import org.cemetery.app.core.*
import org.cemetery.app.ui.*

@Composable fun SettingsScreen(settings:Settings,vm:AppViewModel){
    val context=LocalContext.current;var denied by remember{mutableStateOf(false)};var licenses by remember{mutableStateOf(false)}
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->denied=!granted;vm.reminders(granted)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=24.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        CemeteryPanel(Modifier.fillMaxWidth()){
            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
                Choice(R.string.language,settings.language,listOf("system" to R.string.system_default,"en" to R.string.english,"ru" to R.string.russian)){vm.text("language",it)}
                Choice(R.string.theme,settings.theme,listOf("system" to R.string.system_default,"light" to R.string.light,"dark" to R.string.dark)){vm.text("theme",it)}
            }
        }
        CemeteryPanel(Modifier.fillMaxWidth()){
            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
                    CemeteryText(stringResource(R.string.reminders),Modifier.weight(1f),style=CemeteryDesign.type.label)
                    CemeterySwitch(settings.reminders,{enabled->if(enabled && Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else vm.reminders(enabled)},stringResource(R.string.reminders))
                }
                if(denied)CemeteryText(stringResource(R.string.permission_denied),color=CemeteryDesign.colors.error)
                CemeteryText(stringResource(R.string.reminder_hint),style=CemeteryDesign.type.small,color=CemeteryDesign.colors.secondary)
                Choice(R.string.reminder_lead,settings.leadMinutes.toString(),listOf("15" to R.string.minutes_15,"60" to R.string.hour_1,"1440" to R.string.day_1)){vm.lead(it.toInt())}
            }
        }
        CemeteryPanel(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp)){Choice(R.string.epitaph_style,settings.style,listOf("neutral" to R.string.neutral,"philosophical" to R.string.philosophical,"dark_humor" to R.string.dark_humor)){vm.text("style",it)}}}
        CemeteryPanel(Modifier.fillMaxWidth()){
            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                CemeteryText(stringResource(R.string.about),style=CemeteryDesign.type.button)
                CemeteryText(stringResource(R.string.about_body,BuildConfig.VERSION_NAME),color=CemeteryDesign.colors.secondary)
                CemeteryButton(stringResource(R.string.licenses),{licenses=true},kind=ButtonKind.SECONDARY)
            }
        }
    }
    if(licenses)CemeteryDialog({licenses=false}){
        CemeteryText(stringResource(R.string.licenses),style=CemeteryDesign.type.title)
        CemeteryText(stringResource(R.string.licenses_body))
        CemeteryText(remember{context.assets.open("THIRD_PARTY_NOTICES.txt").bufferedReader().use{it.readText()}+"\n\n"+context.assets.open("fonts/Nunito-OFL.txt").bufferedReader().use{it.readText()}},style=CemeteryDesign.type.small)
        CemeteryButton(stringResource(R.string.close),{licenses=false},Modifier.fillMaxWidth())
    }
}
@Composable private fun Choice(label:Int,selected:String,options:List<Pair<String,Int>>,choose:(String)->Unit){
    var open by remember{mutableStateOf(false)}
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        CemeteryText(stringResource(label),style=CemeteryDesign.type.label)
        CemeteryButton(stringResource(options.first{it.first==selected}.second),{open=true},Modifier.fillMaxWidth(),kind=ButtonKind.SECONDARY,glyph=CemeteryGlyph.CHEVRON)
    }
    if(open)CemeteryDialog({open=false}){
        CemeteryText(stringResource(label),style=CemeteryDesign.type.title)
        options.forEach{(key,res)->CemeteryFilterTab(stringResource(res),key==selected,{choose(key);open=false},Modifier.fillMaxWidth())}
        CemeteryButton(stringResource(R.string.close),{open=false},Modifier.fillMaxWidth(),kind=ButtonKind.QUIET)
    }
}
