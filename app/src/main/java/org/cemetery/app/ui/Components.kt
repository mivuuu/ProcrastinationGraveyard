package org.cemetery.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.window.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import org.cemetery.app.core.*

@Composable fun CemeteryText(text:String,modifier:Modifier=Modifier,style:TextStyle=CemeteryDesign.type.body,color:Color=CemeteryDesign.colors.text,align:TextAlign?=null,maxLines:Int=Int.MAX_VALUE){BasicText(text,modifier,style.copy(color=color,textAlign=align?:TextAlign.Start),maxLines=maxLines)}
@Composable fun CemeteryPanel(modifier:Modifier=Modifier,color:Color=CemeteryDesign.colors.surface,content:@Composable ColumnScope.()->Unit){Column(modifier.clip(CemeteryShapes.card).background(color),content=content)}
enum class ButtonKind { PRIMARY,SECONDARY,QUIET,DANGER }
@Composable fun CemeteryButton(text:String,onClick:()->Unit,modifier:Modifier=Modifier,kind:ButtonKind=ButtonKind.PRIMARY,enabled:Boolean=true,glyph:CemeteryGlyph?=null){
    val c=CemeteryDesign.colors;val source=remember{MutableInteractionSource()};val pressed by source.collectIsPressedAsState();val focused by source.collectIsFocusedAsState()
    val scale by animateFloatAsState(if(pressed).985f else 1f,tween(if(CemeteryDesign.motion)100 else 0),label="press")
    val background=when(kind){ButtonKind.PRIMARY->c.accent;ButtonKind.SECONDARY->c.surface;ButtonKind.QUIET->Color.Transparent;ButtonKind.DANGER->c.errorSurface}
    val foreground=if(!enabled)c.secondary else when(kind){ButtonKind.PRIMARY->c.onAccent;ButtonKind.DANGER->c.error;else->c.text}
    Row(modifier.graphicsLayer{scaleX=scale;scaleY=scale}.heightIn(min=if(kind==ButtonKind.PRIMARY)60.dp else 48.dp).clip(CemeteryShapes.pill)
        .background(if(enabled)background else c.disabled).then(if(kind==ButtonKind.PRIMARY && enabled)Modifier.background(Brush.horizontalGradient(listOf(c.accent,c.accentEnd))) else Modifier)
        .border(if(focused)2.dp else 0.dp,if(focused)c.text else Color.Transparent,CemeteryShapes.pill)
        .clickable(source,null,enabled,role=Role.Button,onClick=onClick).padding(horizontal=20.dp,vertical=12.dp),
        horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically){
        glyph?.let{CemeteryIcon(it,color=foreground,modifier=Modifier.size(if(kind==ButtonKind.PRIMARY)28.dp else 22.dp));Spacer(Modifier.width(10.dp))}
        CemeteryText(text,style=if(kind==ButtonKind.PRIMARY)CemeteryDesign.type.button else CemeteryDesign.type.label,color=foreground,align=TextAlign.Center)
    }
}
@Composable fun CemeteryIconButton(glyph:CemeteryGlyph,description:String,onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,soft:Boolean=true){
    val c=CemeteryDesign.colors;val source=remember{MutableInteractionSource()};val pressed by source.collectIsPressedAsState();val focused by source.collectIsFocusedAsState()
    Box(modifier.sizeIn(minWidth=48.dp,minHeight=48.dp).clip(CemeteryShapes.field).background(if(pressed)c.selected else if(soft)c.surface else Color.Transparent)
        .border(if(focused)2.dp else 0.dp,if(focused)c.text else Color.Transparent,CemeteryShapes.field)
        .semantics{contentDescription=description}.clickable(source,null,enabled,role=Role.Button,onClick=onClick).padding(12.dp),contentAlignment=Alignment.Center){CemeteryIcon(glyph,color=if(enabled)c.text else c.secondary.copy(alpha=.5f))}
}
@Composable fun CemeteryFilterTab(text:String,selected:Boolean,onClick:()->Unit,modifier:Modifier=Modifier,compact:Boolean=false){
    val c=CemeteryDesign.colors
    Row(modifier.heightIn(min=48.dp).clip(CemeteryShapes.pill).background(if(selected)c.accent else c.surface)
        .selectable(selected,onClick=onClick,role=Role.Tab).padding(horizontal=if(compact)4.dp else 18.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){
        CemeteryText(text,style=if(compact)CemeteryDesign.type.label.copy(fontSize=13.sp) else CemeteryDesign.type.label,color=if(selected)c.onAccent else c.text,align=TextAlign.Center)
    }
}
@Composable fun CemeteryTextField(value:String,onChange:(String)->Unit,label:String,modifier:Modifier=Modifier,singleLine:Boolean=true,error:String?=null,keyboard:KeyboardOptions=KeyboardOptions(capitalization=KeyboardCapitalization.Sentences,imeAction=ImeAction.Done)){
    val c=CemeteryDesign.colors;val source=remember{MutableInteractionSource()};val focused by source.collectIsFocusedAsState();val controller=LocalSoftwareKeyboardController.current
    Column(modifier,verticalArrangement=Arrangement.spacedBy(7.dp)){
        CemeteryText(label,style=CemeteryDesign.type.label)
        BasicTextField(value,onChange,Modifier.fillMaxWidth().heightIn(min=54.dp).clip(CemeteryShapes.field).background(c.surface)
            .border(if(focused || error!=null)2.dp else 1.dp,if(error!=null)c.error else if(focused)c.text else c.border,CemeteryShapes.field)
            .padding(16.dp).semantics{contentDescription=label;if(error!=null)this.error(error)},
            textStyle=CemeteryDesign.type.body.copy(color=c.text),singleLine=singleLine,minLines=if(singleLine)1 else 3,maxLines=if(singleLine)1 else 5,
            interactionSource=source,cursorBrush=SolidColor(c.text),keyboardOptions=keyboard,keyboardActions=KeyboardActions(onDone={controller?.hide()}))
        error?.let{CemeteryText(it,style=CemeteryDesign.type.small,color=c.error)}
    }
}
@Composable fun CemeterySwitch(checked:Boolean,onChange:(Boolean)->Unit,label:String,modifier:Modifier=Modifier){
    val c=CemeteryDesign.colors;val progress by animateFloatAsState(if(checked)1f else 0f,tween(if(CemeteryDesign.motion)150 else 0),label="toggle")
    Box(modifier.size(60.dp,48.dp).semantics{contentDescription=label}.toggleable(checked,role=Role.Switch,onValueChange=onChange),contentAlignment=Alignment.Center){
        Box(Modifier.size(56.dp,32.dp).clip(CemeteryShapes.pill).background(if(checked)c.accent else c.border)){
            Box(Modifier.offset(x=(4+24*progress).dp,y=4.dp).size(24.dp).clip(CemeteryShapes.pill).background(if(checked)c.onAccent else c.secondary))
        }
    }
}
@Composable fun CemeteryCheckbox(checked:Boolean,onChange:()->Unit,label:String,modifier:Modifier=Modifier){
    val c=CemeteryDesign.colors
    Box(modifier.size(48.dp).semantics{contentDescription=label}.toggleable(checked,role=Role.Checkbox,onValueChange={onChange()}),contentAlignment=Alignment.Center){
        Box(Modifier.size(30.dp).clip(CemeteryShapes.pill).background(if(checked)c.accent else c.elevated).border(2.dp,if(checked)c.accent else c.border,CemeteryShapes.pill),contentAlignment=Alignment.Center){if(checked)CemeteryIcon(CemeteryGlyph.CHECK,color=c.onAccent,modifier=Modifier.size(20.dp))}
    }
}
@Composable fun CemeteryDivider(){Box(Modifier.fillMaxWidth().height(1.dp).background(CemeteryDesign.colors.border.copy(alpha=.6f)))}
@Composable fun CemeteryLoading(modifier:Modifier=Modifier){
    val c=CemeteryDesign.colors
    val motion=CemeteryDesign.motion
    val transition=rememberInfiniteTransition(label="loading")
    val angle by transition.animateFloat(0f,360f,infiniteRepeatable(tween(1100,easing=LinearEasing)),label="angle")
    androidx.compose.foundation.Canvas(modifier.size(36.dp)){drawArc(c.accent,if(motion)angle else 0f,260f,false,style=androidx.compose.ui.graphics.drawscope.Stroke(4.dp.toPx(),cap=StrokeCap.Round))}
}
/** The custom scrim owns dimming; Android's default dialog dim must not darken it twice. */
@Composable private fun CemeteryDialogWindow(){
    val view=LocalView.current;val dark=CemeteryDesign.colors.dark
    SideEffect {
        (view.parent as? DialogWindowProvider)?.window?.let{window->
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(0f)
            window.statusBarColor=android.graphics.Color.TRANSPARENT
            window.navigationBarColor=android.graphics.Color.TRANSPARENT
            if(android.os.Build.VERSION.SDK_INT>=29)window.isNavigationBarContrastEnforced=false
            androidx.core.view.WindowCompat.getInsetsController(window,window.decorView).apply{
                isAppearanceLightStatusBars=!dark;isAppearanceLightNavigationBars=!dark
            }
        }
    }
}
@Composable fun CemeteryDialog(onDismiss:()->Unit,content:@Composable ColumnScope.()->Unit){
    val c=CemeteryDesign.colors
    Dialog(onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)){
        CemeteryDialogWindow()
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.42f)).windowInsetsPadding(WindowInsets.safeDrawing).imePadding().padding(20.dp).pointerInput(onDismiss){detectTapGestures{onDismiss()}},contentAlignment=Alignment.Center){
            Column(Modifier.widthIn(max=560.dp).fillMaxWidth().heightIn(max=maxHeight).clip(CemeteryShapes.card).background(c.elevated)
                .pointerInput(Unit){detectTapGestures{}}.verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp),content=content)
        }
    }
}
@OptIn(ExperimentalComposeUiApi::class)
@Composable fun CemeteryBottomSheet(onDismiss:()->Unit,modifier:Modifier=Modifier,fraction:Float=.44f,tag:String?=null,content:@Composable ColumnScope.()->Unit){
    val c=CemeteryDesign.colors;val height=(LocalConfiguration.current.screenHeightDp*fraction).dp
    // Resolve host insets before entering the separate Dialog window.
    val navigation=WindowInsets.navigationBars.asPaddingValues()
    val safeBottom=navigation.calculateBottomPadding()
    val direction=LocalLayoutDirection.current
    val safeStart=navigation.calculateStartPadding(direction);val safeEnd=navigation.calculateEndPadding(direction)
    var drag by remember{mutableFloatStateOf(0f)};val threshold=with(LocalDensity.current){64.dp.toPx()}
    Dialog(onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)){
        CemeteryDialogWindow()
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.12f)).semantics{testTagsAsResourceId=true}.pointerInput(onDismiss){detectTapGestures{onDismiss()}}){
            Column(modifier.align(Alignment.BottomCenter).padding(start=safeStart,end=safeEnd,bottom=safeBottom).widthIn(max=640.dp).fillMaxWidth().offset{IntOffset(0,drag.toInt())}
                .height(height).clip(CemeteryShapes.sheet).background(c.elevated).then(if(tag!=null)Modifier.testTag(tag) else Modifier).pointerInput(Unit){detectTapGestures{}}){
                Box(Modifier.fillMaxWidth().height(36.dp).pointerInput(onDismiss){detectVerticalDragGestures(onVerticalDrag={change,amount->change.consume();drag=(drag+amount).coerceAtLeast(0f)},onDragEnd={if(drag>threshold)onDismiss();drag=0f},onDragCancel={drag=0f})},contentAlignment=Alignment.Center){Box(Modifier.size(36.dp,4.dp).clip(CemeteryShapes.pill).background(c.secondary))}
                content()
            }
        }
    }
}
