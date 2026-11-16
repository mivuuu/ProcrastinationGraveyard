package org.cemetery.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.semantics.clearAndSetSemantics
import org.cemetery.app.core.CemeteryDesign

/** Original vector artwork. Decorative: the adjacent localized text carries the empty-state meaning. */
@Composable fun SproutIllustration(modifier:Modifier=Modifier){
    val c=CemeteryDesign.colors
    Canvas(modifier.clearAndSetSemantics{}){
        val factor=minOf(size.width/320f,size.height/220f)
        translate((size.width-320*factor)/2,(size.height-220*factor)/2){scale(factor,pivot=Offset.Zero){
            val bush=c.bush;val lime=c.accent;val ink=c.onAccent
            drawCircle(bush,29f,Offset(47f,166f));drawCircle(bush,19f,Offset(80f,172f));drawCircle(bush,14f,Offset(103f,175f))
            drawCircle(bush,31f,Offset(265f,163f));drawCircle(bush,20f,Offset(292f,179f));drawCircle(bush,15f,Offset(226f,176f))
            drawOval(Brush.verticalGradient(listOf(lime,androidx.compose.ui.graphics.lerp(lime,c.selected,.12f))),Offset(104f,84f),Size(121f,121f))
            drawPath(Path().apply{moveTo(167f,87f);cubicTo(137f,76f,144f,41f,151f,34f);cubicTo(177f,39f,182f,69f,174f,88f);close()},androidx.compose.ui.graphics.lerp(lime,bush,.32f))
            drawPath(Path().apply{moveTo(174f,87f);cubicTo(173f,60f,201f,40f,224f,49f);cubicTo(221f,76f,202f,93f,174f,87f);close()},androidx.compose.ui.graphics.lerp(lime,bush,.24f))
            drawLine(ink.copy(alpha=.65f),Offset(170f,86f),Offset(160f,57f),3.8f,StrokeCap.Round)
            drawLine(ink.copy(alpha=.65f),Offset(178f,84f),Offset(204f,62f),3.8f,StrokeCap.Round)
            val face=Stroke(3.7f,cap=StrokeCap.Round,join=StrokeJoin.Round)
            drawPath(Path().apply{moveTo(136f,126f);cubicTo(139f,117f,148f,117f,151f,126f)},ink,style=face)
            drawPath(Path().apply{moveTo(177f,128f);cubicTo(180f,119f,189f,119f,192f,128f)},ink,style=face)
            drawPath(Path().apply{moveTo(155f,135f);cubicTo(160f,141f,167f,142f,172f,136f)},ink,style=face)
            drawPath(Path().apply{moveTo(11f,197f);cubicTo(93f,156f,218f,151f,309f,198f);lineTo(309f,220f);lineTo(11f,220f);close()},Brush.verticalGradient(listOf(c.hill,c.backgroundEnd),155f,220f))
            fun hand(x:Float,y:Float,mirror:Boolean){
                drawOval(lime,Offset(x,y),Size(31f,24f))
                drawPath(Path().apply{moveTo(x+if(mirror)4f else 27f,y+4);cubicTo(x+if(mirror)35f else -5f,y-8,x+if(mirror)36f else -4f,y+32,x+if(mirror)6f else 27f,y+22)},ink,style=Stroke(3.1f,cap=StrokeCap.Round))
            }
            hand(113f,153f,false);hand(185f,154f,true)
            listOf(floatArrayOf(91f,110f,102f,113f),floatArrayOf(94f,96f,105f,102f),floatArrayOf(109f,86f,113f,96f)).forEach{drawLine(c.yellow,Offset(it[0],it[1]),Offset(it[2],it[3]),5f,StrokeCap.Round)}
        }}
    }
}
@Composable fun CemeterySpark(modifier:Modifier=Modifier){
    val color=CemeteryDesign.colors.yellow
    Canvas(modifier.clearAndSetSemantics{}){
        scale(size.width/32,size.height/32,pivot=Offset.Zero){
            drawLine(color,Offset(7f,4f),Offset(4f,13f),4.5f,StrokeCap.Round)
            drawLine(color,Offset(21f,12f),Offset(13f,18f),4.5f,StrokeCap.Round)
            drawLine(color,Offset(27f,26f),Offset(18f,26f),4.5f,StrokeCap.Round)
        }
    }
}
