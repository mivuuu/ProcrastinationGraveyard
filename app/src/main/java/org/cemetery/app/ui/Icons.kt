package org.cemetery.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import org.cemetery.app.core.CemeteryDesign
import kotlin.math.*

enum class CemeteryGlyph { SETTINGS,TASKS,GRAVE,PLUS,CHECK,DELETE,BACK,CLOSE,MORE,CALENDAR,CLOCK,LIST,LEAF,FLOWER,CHEVRON }
@Composable fun CemeteryIcon(glyph:CemeteryGlyph,description:String?=null,modifier:Modifier=Modifier,color:Color=CemeteryDesign.colors.text){
    Canvas(modifier.size(26.dp).then(if(description==null)Modifier.clearAndSetSemantics{} else Modifier.semantics{contentDescription=description})){
        scale(size.width/24,size.height/24,pivot=Offset.Zero){
            val stroke=Stroke(2.2f,cap=StrokeCap.Round,join=StrokeJoin.Round)
            fun line(a:Float,b:Float,c:Float,d:Float)=drawLine(color,Offset(a,b),Offset(c,d),2.2f,StrokeCap.Round)
            fun path(vararg points:Pair<Float,Float>){drawPath(Path().apply{points.forEachIndexed{i,p->if(i==0)moveTo(p.first,p.second) else lineTo(p.first,p.second)}},color,style=stroke)}
            when(glyph){
                CemeteryGlyph.SETTINGS->{
                    val p=Path();repeat(32){i->val a=i*PI.toFloat()/16;val r=if(i%4==1 || i%4==2)9.7f else 7.6f;val x=12+cos(a)*r;val y=12+sin(a)*r;if(i==0)p.moveTo(x,y) else p.lineTo(x,y)};p.close();drawPath(p,color,style=stroke);drawCircle(color,3.3f,Offset(12f,12f),style=stroke)
                }
                CemeteryGlyph.TASKS->{drawCircle(color,9f,Offset(12f,12f),style=stroke);path(8f to 12f,11f to 15f,16f to 9f)}
                CemeteryGlyph.GRAVE->{drawPath(Path().apply{moveTo(5f,20f);lineTo(5f,12f);cubicTo(5f,2f,19f,2f,19f,12f);lineTo(19f,20f)},color,style=stroke);line(3f,20f,21f,20f)}
                CemeteryGlyph.PLUS->{line(12f,4f,12f,20f);line(4f,12f,20f,12f)}
                CemeteryGlyph.CHECK->path(5f to 12f,10f to 17f,20f to 6f)
                CemeteryGlyph.BACK->path(15f to 5f,8f to 12f,15f to 19f)
                CemeteryGlyph.CHEVRON->path(9f to 6f,15f to 12f,9f to 18f)
                CemeteryGlyph.CLOSE->{line(6f,6f,18f,18f);line(18f,6f,6f,18f)}
                CemeteryGlyph.DELETE->{line(4f,6f,20f,6f);path(6f to 6f,7f to 21f,17f to 21f,18f to 6f);path(9f to 6f,9f to 3f,15f to 3f,15f to 6f);line(10f,10f,10f,17f);line(14f,10f,14f,17f)}
                CemeteryGlyph.MORE->repeat(3){drawCircle(color,1.5f,Offset(12f,6f+it*6))}
                CemeteryGlyph.CALENDAR->{drawRoundRect(color,Offset(3f,5f),Size(18f,16f),CornerRadius(3f),style=stroke);line(7f,3f,7f,7f);line(17f,3f,17f,7f);line(3f,10f,21f,10f)}
                CemeteryGlyph.CLOCK->{drawCircle(color,9f,Offset(12f,12f),style=stroke);path(12f to 6f,12f to 12f,16f to 14f)}
                CemeteryGlyph.LIST->repeat(3){i->drawCircle(color,1f,Offset(4f,6f+i*6));line(9f,6f+i*6,21f,6f+i*6)}
                CemeteryGlyph.LEAF->{drawPath(Path().apply{moveTo(12f,21f);cubicTo(0f,15f,1f,7f,4f,5f);cubicTo(10f,6f,14f,12f,12f,21f);moveTo(12f,18f);cubicTo(10f,7f,16f,2f,22f,3f);cubicTo(22f,13f,18f,18f,12f,18f)},color,style=stroke)}
                CemeteryGlyph.FLOWER->{repeat(5){i->val a=i*2*PI.toFloat()/5;drawCircle(color,3.5f,Offset(12+cos(a)*5,9+sin(a)*5),style=Stroke(1.6f))};line(12f,13f,12f,22f)}
            }
        }
    }
}
