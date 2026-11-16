package org.cemetery.app.ui

import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.layout.onSizeChanged
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
    val c=CemeteryDesign.colors;val usable=enabled && !LocalOverlayClosing.current;val source=remember{MutableInteractionSource()};val pressed by source.collectIsPressedAsState();val focused by source.collectIsFocusedAsState()
    val scale by animateFloatAsState(if(pressed).985f else 1f,CemeteryMotion.spec(CemeteryMotion.PRESS),label="press")
    val background=when(kind){ButtonKind.PRIMARY->c.accent;ButtonKind.SECONDARY->c.surface;ButtonKind.QUIET->Color.Transparent;ButtonKind.DANGER->c.errorSurface}
    val foreground=if(!enabled)c.secondary else when(kind){ButtonKind.PRIMARY->c.onAccent;ButtonKind.DANGER->c.error;else->c.text}
    Row(modifier.heightIn(min=if(kind==ButtonKind.PRIMARY)60.dp else 48.dp).clickable(source,null,usable,role=Role.Button,onClick=onClick).graphicsLayer{scaleX=scale;scaleY=scale}.clip(CemeteryShapes.pill)
        .background(if(enabled)background else c.disabled).then(if(kind==ButtonKind.PRIMARY && enabled)Modifier.background(Brush.horizontalGradient(listOf(c.accent,c.accentEnd))) else Modifier)
        .border(if(focused)2.dp else 0.dp,if(focused)c.text else Color.Transparent,CemeteryShapes.pill)
        .padding(horizontal=20.dp,vertical=12.dp),
        horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically){
        glyph?.let{CemeteryIcon(it,color=foreground,modifier=Modifier.size(if(kind==ButtonKind.PRIMARY)28.dp else 22.dp));Spacer(Modifier.width(10.dp))}
        CemeteryText(text,style=if(kind==ButtonKind.PRIMARY)CemeteryDesign.type.button else CemeteryDesign.type.label,color=foreground,align=TextAlign.Center)
    }
}
@Composable fun CemeteryIconButton(glyph:CemeteryGlyph,description:String,onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,soft:Boolean=true){
    val c=CemeteryDesign.colors;val usable=enabled && !LocalOverlayClosing.current;val source=remember{MutableInteractionSource()};val pressed by source.collectIsPressedAsState();val focused by source.collectIsFocusedAsState()
    val scale by animateFloatAsState(if(pressed).96f else 1f,CemeteryMotion.spec(CemeteryMotion.PRESS),label="icon-press")
    val bg by animateColorAsState(if(pressed)c.selected else if(soft)c.surface else Color.Transparent,CemeteryMotion.spec(CemeteryMotion.PRESS),label="icon-background")
    Box(modifier.sizeIn(minWidth=48.dp,minHeight=48.dp).semantics{contentDescription=description}.clickable(source,null,usable,role=Role.Button,onClick=onClick).graphicsLayer{scaleX=scale;scaleY=scale}.clip(CemeteryShapes.field).background(bg)
        .border(if(focused)2.dp else 0.dp,if(focused)c.text else Color.Transparent,CemeteryShapes.field)
        .padding(12.dp),contentAlignment=Alignment.Center){CemeteryIcon(glyph,color=if(enabled)c.text else c.secondary.copy(alpha=.5f))}
}
@Composable fun CemeteryFilterTab(text:String,selected:Boolean,onClick:()->Unit,modifier:Modifier=Modifier,compact:Boolean=false){
    val c=CemeteryDesign.colors
    val source=remember{MutableInteractionSource()};val pressed by source.collectIsPressedAsState()
    val bg by animateColorAsState(if(selected)c.accent else if(pressed)c.selected else c.surface,CemeteryMotion.spec(CemeteryMotion.SELECTION),label="filter-background")
    val fg by animateColorAsState(if(selected)c.onAccent else c.text,CemeteryMotion.spec(CemeteryMotion.SELECTION),label="filter-label")
    Row(modifier.heightIn(min=48.dp).clip(CemeteryShapes.pill).background(bg)
        .selectable(selected,interactionSource=source,indication=null,enabled=!LocalOverlayClosing.current,onClick=onClick,role=Role.Tab).padding(horizontal=if(compact)4.dp else 18.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){
        CemeteryText(text,style=if(compact)CemeteryDesign.type.label.copy(fontSize=13.sp) else CemeteryDesign.type.label,color=fg,align=TextAlign.Center)
    }
}
@Composable fun CemeteryTextField(value:String,onChange:(String)->Unit,label:String,modifier:Modifier=Modifier,singleLine:Boolean=true,error:String?=null,keyboard:KeyboardOptions=KeyboardOptions(capitalization=KeyboardCapitalization.Sentences,imeAction=ImeAction.Done)){
    val c=CemeteryDesign.colors;val source=remember{MutableInteractionSource()};val focused by source.collectIsFocusedAsState();val controller=LocalSoftwareKeyboardController.current
    Column(modifier,verticalArrangement=Arrangement.spacedBy(7.dp)){
        CemeteryText(label,style=CemeteryDesign.type.label)
        BasicTextField(value,onChange,Modifier.fillMaxWidth().heightIn(min=54.dp).clip(CemeteryShapes.field).background(c.surface)
            .border(if(focused || error!=null)2.dp else 1.dp,if(error!=null)c.error else if(focused)c.text else c.border,CemeteryShapes.field)
            .padding(16.dp).semantics{contentDescription=label;if(error!=null)this.error(error)},
            textStyle=CemeteryDesign.type.body.copy(color=c.text),enabled=!LocalOverlayClosing.current,singleLine=singleLine,minLines=if(singleLine)1 else 3,maxLines=if(singleLine)1 else 5,
            interactionSource=source,cursorBrush=SolidColor(c.text),keyboardOptions=keyboard,keyboardActions=KeyboardActions(onDone={controller?.hide()}))
        error?.let{CemeteryText(it,style=CemeteryDesign.type.small,color=c.error)}
    }
}
@Composable fun CemeterySwitch(checked:Boolean,onChange:(Boolean)->Unit,label:String,modifier:Modifier=Modifier){
    val c=CemeteryDesign.colors
    val source=remember{MutableInteractionSource()};val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if(pressed).96f else 1f,CemeteryMotion.spec(CemeteryMotion.PRESS),label="switch-press")
    val track by animateColorAsState(if(checked)c.accent else c.border,CemeteryMotion.spec(CemeteryMotion.SELECTION),label="switch-track")
    val thumb by animateColorAsState(if(checked)c.onAccent else c.secondary,CemeteryMotion.spec(CemeteryMotion.SELECTION),label="switch-thumb")
    val progress by animateFloatAsState(if(checked)1f else 0f,CemeteryMotion.spec(CemeteryMotion.SELECTION),label="toggle")
    Box(modifier.size(60.dp,48.dp).semantics{contentDescription=label}.toggleable(checked,interactionSource=source,indication=null,enabled=!LocalOverlayClosing.current,role=Role.Switch,onValueChange=onChange),contentAlignment=Alignment.Center){
        Box(Modifier.size(56.dp,32.dp).graphicsLayer{scaleX=scale;scaleY=scale}.clip(CemeteryShapes.pill).background(track)){
            Box(Modifier.offset(x=(4+24*progress).dp,y=4.dp).size(24.dp).clip(CemeteryShapes.pill).background(thumb))
        }
    }
}
@Composable fun CemeteryCheckbox(checked:Boolean,onChange:()->Unit,label:String,modifier:Modifier=Modifier){
    val c=CemeteryDesign.colors
    val source=remember{MutableInteractionSource()};val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if(pressed).95f else 1f,CemeteryMotion.spec(CemeteryMotion.PRESS),label="check-press")
    val progress by animateFloatAsState(if(checked)1f else 0f,CemeteryMotion.spec(CemeteryMotion.SELECTION),label="check-progress")
    Box(modifier.size(48.dp).semantics{contentDescription=label}.toggleable(checked,interactionSource=source,indication=null,enabled=!LocalOverlayClosing.current,role=Role.Checkbox,onValueChange={onChange()}),contentAlignment=Alignment.Center){
        Box(Modifier.size(30.dp).graphicsLayer{scaleX=scale;scaleY=scale}.clip(CemeteryShapes.pill).background(lerp(c.elevated,c.accent,progress)).border(2.dp,lerp(c.border,c.accent,progress),CemeteryShapes.pill),contentAlignment=Alignment.Center){if(progress>0f)CemeteryIcon(CemeteryGlyph.CHECK,color=c.onAccent.copy(alpha=progress),modifier=Modifier.size(20.dp).graphicsLayer{scaleX=.8f+.2f*progress;scaleY=scaleX})}
    }
}
@Composable fun CemeteryDivider(){Box(Modifier.fillMaxWidth().height(1.dp).background(CemeteryDesign.colors.border.copy(alpha=.6f)))}
@Composable fun CemeteryLoading(modifier:Modifier=Modifier){
    val c=CemeteryDesign.colors
    val motion=CemeteryDesign.motion
    val angle:State<Float> = if(motion)rememberInfiniteTransition(label="loading").animateFloat(0f,360f,infiniteRepeatable(tween(1100,easing=LinearEasing)),label="angle") else remember{mutableFloatStateOf(0f)}
    androidx.compose.foundation.Canvas(modifier.size(36.dp)){drawArc(c.accent,angle.value,260f,false,style=androidx.compose.ui.graphics.drawscope.Stroke(4.dp.toPx(),cap=StrokeCap.Round))}
}
/** The custom scrim owns dimming; Android's default dialog dim must not darken it twice. */
@Composable private fun CemeteryDialogWindow(fullScreen:Boolean=false){
    val view=LocalView.current;val dark=CemeteryDesign.colors.dark
    SideEffect {
        (view.parent as? DialogWindowProvider)?.window?.let{window->
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(0f)
            window.attributes=window.attributes.apply{windowAnimations=0}
            if(fullScreen)window.setLayout(android.view.WindowManager.LayoutParams.MATCH_PARENT,android.view.WindowManager.LayoutParams.MATCH_PARENT)
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
    val (motion,opacity)=rememberOverlayMotion()
    Dialog({motion.exit(onDismiss)},properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)){
        CemeteryDialogWindow()
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.42f*opacity.value)).windowInsetsPadding(WindowInsets.safeDrawing).imePadding().padding(20.dp).pointerInput(onDismiss){detectTapGestures{motion.exit(onDismiss)}},contentAlignment=Alignment.Center){
            Column(Modifier.widthIn(max=560.dp).fillMaxWidth().heightIn(max=maxHeight).graphicsLayer{alpha=opacity.value;scaleX=.985f+.015f*opacity.value;scaleY=scaleX;translationY=(1-opacity.value)*12.dp.toPx()}.clip(CemeteryShapes.card).background(c.elevated)
                .pointerInput(Unit){detectTapGestures{}}.verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){OverlayMotionContent(motion){content()}}
        }
    }
}
@OptIn(ExperimentalComposeUiApi::class)
@Composable fun CemeteryBottomSheet(onDismiss:()->Unit,modifier:Modifier=Modifier,fraction:Float=.44f,tag:String?=null,content:@Composable ColumnScope.()->Unit){
    val c=CemeteryDesign.colors
    val (motion,opacity)=rememberOverlayMotion()
    var sheetPixels by remember{mutableIntStateOf(0)}
    var drag by remember{mutableFloatStateOf(0f)}
    var dragging by remember{mutableStateOf(false)}
    val resting by animateFloatAsState(drag,if(dragging)snap() else CemeteryMotion.spec(CemeteryMotion.PRESS),label="sheet-settle")
    val threshold=with(LocalDensity.current){64.dp.toPx()}
    Dialog({motion.exit(onDismiss)},properties=DialogProperties(usePlatformDefaultWidth=true,decorFitsSystemWindows=true)){
        CemeteryDialogWindow(fullScreen=true)
        // Compose 1.7.8's usePlatformDefaultWidth=false replaces the measured height with
        // configuration.screenHeightDp. Use fitted MATCH_PARENT window constraints instead.
        // Consume only the safe drawing insets left after the platform fits the system bars.
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.12f*opacity.value)).windowInsetsPadding(WindowInsets.safeDrawing).semantics{testTagsAsResourceId=true}.pointerInput(onDismiss){detectTapGestures{motion.exit(onDismiss)}}){
            val sheetHeight=maxHeight*fraction
            Box(Modifier.align(Alignment.BottomCenter)){
            Column(modifier.widthIn(max=640.dp).fillMaxWidth().height(sheetHeight).onSizeChanged{sheetPixels=it.height}
                .graphicsLayer{alpha=opacity.value;translationY=resting+(1-opacity.value)*sheetPixels*.16f}.clip(CemeteryShapes.sheet).background(c.elevated).then(if(tag!=null)Modifier.testTag(tag) else Modifier).pointerInput(Unit){detectTapGestures{}}){
                Box(Modifier.fillMaxWidth().height(36.dp).pointerInput(onDismiss){detectVerticalDragGestures(onDragStart={dragging=true},onVerticalDrag={change,amount->change.consume();drag=(drag+amount).coerceAtLeast(0f)},onDragEnd={dragging=false;if(drag>threshold)motion.exit(onDismiss) else drag=0f},onDragCancel={dragging=false;drag=0f})},contentAlignment=Alignment.Center){Box(Modifier.size(36.dp,4.dp).clip(CemeteryShapes.pill).background(c.secondary))}
                OverlayMotionContent(motion){content()}
            }
            }
        }
    }
}
