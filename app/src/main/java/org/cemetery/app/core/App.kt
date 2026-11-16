package org.cemetery.app.core
import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import io.github.sceneview.rememberEngine
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import org.cemetery.app.ui.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.*
import androidx.lifecycle.compose.*
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import kotlinx.coroutines.delay
import org.cemetery.app.R
import org.cemetery.app.domain.*
import org.cemetery.app.feature.tasks.*
import org.cemetery.app.feature.cemetery.*
import org.cemetery.app.feature.settings.*
val LocalHostActivity = staticCompositionLocalOf<androidx.activity.ComponentActivity?> { null }
@Composable fun CemeteryApp(vm: AppViewModel=hiltViewModel()) {
    val loadedSettings by vm.settings.collectAsStateWithLifecycle()
    val settings=loadedSettings
    if(settings==null){
        CemeteryTheme(isSystemInDarkTheme()){CemeteryBackground{Box(Modifier.fillMaxSize(),contentAlignment=androidx.compose.ui.Alignment.Center){CemeteryLoading()}}}
        return
    }
    val base=LocalContext.current
    val context=base
    val systemLanguage=android.content.res.Resources.getSystem().configuration.locales[0].language
    val language=if(settings.language=="system")systemLanguage.takeIf{it=="ru" || it=="en"}?:"en" else settings.language
    LaunchedEffect(language) {
        if(androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags()!=language)
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.forLanguageTags(language))
    }
    val dark=when(settings.theme){"dark"->true;"light"->false;else->isSystemInDarkTheme()}
    CompositionLocalProvider(LocalContext provides context) {
        SideEffect {
            (base as? android.app.Activity)?.window?.let { window ->
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window,false)
                window.statusBarColor=android.graphics.Color.TRANSPARENT
                window.navigationBarColor=android.graphics.Color.TRANSPARENT
                if(android.os.Build.VERSION.SDK_INT>=29)window.isNavigationBarContrastEnforced=false
                androidx.core.view.WindowCompat.getInsetsController(window,window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
        CemeteryTheme(dark) {
            val snapshot by vm.snapshot.collectAsStateWithLifecycle()
            val now by vm.now.collectAsStateWithLifecycle()
            val error by vm.errors.collectAsStateWithLifecycle()
            val owner=LocalLifecycleOwner.current
            LaunchedEffect(owner) {owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){while(true){vm.refresh();delay(15_000)}}}
            // Retain GPU resources and world geometry; only the Android surface is destination-scoped.
            val cemeteryEngine=rememberEngine()
            val renderStore=remember(cemeteryEngine){CemeteryRenderStore(cemeteryEngine,base.applicationContext)}
            DisposableEffect(renderStore){onDispose{renderStore.close()}}
            val nav=rememberNavController()
            val entry by nav.currentBackStackEntryAsState()
            val route=entry?.destination?.route ?: "tasks"
            var editor by rememberSaveable {mutableStateOf(false)}
            var editingId by rememberSaveable {mutableStateOf<String?>(null)}
            var deleteId by rememberSaveable {mutableStateOf<String?>(null)}
            var selectedId by rememberSaveable {mutableStateOf<String?>(null)}
            var presentationReadyId by remember {mutableStateOf<String?>(null)}
            var presentationBusy by remember {mutableStateOf(false)}
            val camera=rememberSaveable(saver=CameraController.Saver) {CameraController()}
            fun tab(destination: String) {nav.navigate(destination){popUpTo(nav.graph.findStartDestination().id){saveState=true};launchSingleTop=true;restoreState=true}}
            fun select(grave: Grave) {if(route!="cemetery" || !presentationBusy){presentationReadyId=null;selectedId=grave.id;if(route!="cemetery")tab("cemetery")}}
            val selected=snapshot.graves.find{it.id==selectedId}
            CemeteryBackground {
                Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                    CemeteryHeader(stringResource(when(route){"settings"->R.string.settings;"cemetery"->R.string.cemetery;else->R.string.tasks}),
                        settings=if(route!="settings")({selectedId=null;presentationReadyId=null;nav.navigate("settings"){launchSingleTop=true}}) else null,
                        back=if(route=="settings")({nav.popBackStack()}) else null)
                    Box(Modifier.weight(1f)) {
                        // Never retain two destination renderers during a NavHost crossfade.
                        // Animate native content and readiness UI independently of the renderer.
                        NavHost(nav,startDestination="tasks",enterTransition={EnterTransition.None},exitTransition={ExitTransition.None},popEnterTransition={EnterTransition.None},popExitTransition={ExitTransition.None}) {
                            composable("tasks"){CemeteryContentReveal{TaskScreen(snapshot,now,edit={editingId=it?.id;editor=true},complete={vm.complete(it.id)},visit={select(it)},remove={deleteId=it.id})}}
                            composable("cemetery"){CemeteryScreen(renderStore,snapshot,now,dark,camera,selectedId,presentationBusy,onSelect={select(it)},onPresentationState={id,ready->presentationBusy=id!=null;presentationReadyId=if(ready)id else null;if(id==null && selectedId!=null && snapshot.graves.none{it.id==selectedId})selectedId=null})}
                            composable("settings"){CemeteryContentReveal{SettingsScreen(settings,vm)}}
                        }
                    }
                    CemeteryNavigationBar(route,tasks={selectedId=null;tab("tasks")},cemetery={tab("cemetery")})
                }
            }
            if(error)CemeteryDialog({vm.errors.value=false}){
                CemeteryText(stringResource(R.string.error_generic),style=CemeteryDesign.type.title)
                CemeteryDismissButton(stringResource(R.string.dismiss),{vm.errors.value=false},Modifier.fillMaxWidth())
            }
            if(selectedId!=null && presentationReadyId==null)BackHandler{selectedId=null}
            if(editor)TaskEditor(snapshot.tasks.find{it.id==editingId},close={editor=false},save=vm::save)
            if(selected!=null && route=="cemetery" && presentationReadyId==selectedId) snapshot.tasks.find{it.id==selected.taskId}?.let {task ->GraveSheet(selected,task,snapshot.history.filter{it.taskId==task.id},now,close={selectedId=null},complete={vm.complete(task.id)},edit={editingId=task.id;selectedId=null;editor=true},remove={deleteId=task.id})}
            if(deleteId!=null)CemeteryDialog({deleteId=null}){
                val exit=LocalOverlayExit.current
                CemeteryText(stringResource(R.string.delete_title),style=CemeteryDesign.type.title)
                CemeteryText(stringResource(R.string.delete_message),color=CemeteryDesign.colors.secondary)
                CemeteryButton(stringResource(R.string.delete),{deleteId?.let(vm::delete);if(selected?.taskId==deleteId)selectedId=null;exit{deleteId=null}},Modifier.fillMaxWidth(),kind=ButtonKind.DANGER)
                CemeteryDismissButton(stringResource(R.string.cancel),{deleteId=null},Modifier.fillMaxWidth(),kind=ButtonKind.QUIET)
            }
        }
    }
}
