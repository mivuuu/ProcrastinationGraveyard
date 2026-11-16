package org.cemetery.app.core

import android.animation.ValueAnimator
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import androidx.compose.foundation.shape.RoundedCornerShape
import org.cemetery.app.R

@Immutable data class CemeteryColors(
    val background:Color,val backgroundEnd:Color,val text:Color,val secondary:Color,
    val accent:Color,val accentEnd:Color,val onAccent:Color,val surface:Color,val elevated:Color,
    val navigation:Color,val selected:Color,val border:Color,val disabled:Color,val error:Color,
    val errorSurface:Color,val hill:Color,val bush:Color,val yellow:Color,val shadow:Color,val dark:Boolean
)
val LightCemeteryColors=CemeteryColors(
    Color(0xFFFFFEF6),Color(0xFFF7FAEE),Color(0xFF133D2B),Color(0xFF58735F),
    Color(0xFFC6ED71),Color(0xFFB9E65E),Color(0xFF123B29),Color(0xFFEDF5E1),Color(0xFFF5F9ED),
    Color(0xFFEBF4DF),Color(0xFFD2EF91),Color(0xFFC8DDBB),Color(0xFFCCD7C2),Color(0xFF934837),
    Color(0xFFF8E9DB),Color(0xFFEBF5DA),Color(0xFF9EC777),Color(0xFFFFD85B),Color(0x12173827),false
)
val DarkCemeteryColors=CemeteryColors(
    Color(0xFF0C3026),Color(0xFF103A2D),Color(0xFFF3F7E9),Color(0xFFB9D0B8),
    Color(0xFFC6ED71),Color(0xFFB9E65E),Color(0xFF123B29),Color(0xFF194B3A),Color(0xFF154333),
    Color(0xFF153E30),Color(0xFF2C6547),Color(0xFF3A7054),Color(0xFF38634C),Color(0xFFFFBCA4),
    Color(0xFF543E30),Color(0xFF10372A),Color(0xFF306C49),Color(0xFFFFD85B),Color(0x30001811),true
)
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private val RoundedFont=FontFamily(
    Font(R.font.nunito,FontWeight.Medium,variationSettings=FontVariation.Settings(FontVariation.weight(500))),Font(R.font.nunito,FontWeight.SemiBold,variationSettings=FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.nunito,FontWeight.Bold,variationSettings=FontVariation.Settings(FontVariation.weight(700))),Font(R.font.nunito,FontWeight.ExtraBold,variationSettings=FontVariation.Settings(FontVariation.weight(800))),Font(R.font.nunito,FontWeight.Black,variationSettings=FontVariation.Settings(FontVariation.weight(900)))
)
@Immutable class CemeteryTypography {
    val screen=TextStyle(fontFamily=RoundedFont,fontWeight=FontWeight.Black,fontSize=34.sp,lineHeight=40.sp)
    val heading=TextStyle(fontFamily=RoundedFont,fontWeight=FontWeight.ExtraBold,fontSize=25.sp,lineHeight=31.sp)
    val title=TextStyle(fontFamily=RoundedFont,fontWeight=FontWeight.ExtraBold,fontSize=23.sp,lineHeight=28.sp)
    val body=TextStyle(fontFamily=RoundedFont,fontWeight=FontWeight.SemiBold,fontSize=17.sp,lineHeight=23.sp)
    val label=TextStyle(fontFamily=RoundedFont,fontWeight=FontWeight.ExtraBold,fontSize=16.sp,lineHeight=21.sp)
    val button=TextStyle(fontFamily=RoundedFont,fontWeight=FontWeight.ExtraBold,fontSize=20.sp,lineHeight=25.sp)
    val small=TextStyle(fontFamily=RoundedFont,fontWeight=FontWeight.SemiBold,fontSize=14.sp,lineHeight=19.sp)
}
object CemeteryShapes {val pill=RoundedCornerShape(1000.dp);val card=RoundedCornerShape(28.dp);val field=RoundedCornerShape(22.dp);val sheet=RoundedCornerShape(topStart=32.dp,topEnd=32.dp)}
object CemeterySpacing {val gutter=24.dp;val small=8.dp;val medium=16.dp;val large=24.dp;val touch=48.dp}
private val LocalColors=staticCompositionLocalOf{LightCemeteryColors}
private val LocalTypography=staticCompositionLocalOf{CemeteryTypography()}
private val LocalMotion=staticCompositionLocalOf{true}
object CemeteryDesign {
    val colors: CemeteryColors @Composable get()=LocalColors.current
    val type: CemeteryTypography @Composable get()=LocalTypography.current
    val motion: Boolean @Composable get()=LocalMotion.current
}
@Composable fun CemeteryTheme(dark:Boolean,content: @Composable () -> Unit) {
    val typography=remember{CemeteryTypography()}
    CompositionLocalProvider(LocalColors provides if(dark)DarkCemeteryColors else LightCemeteryColors,
        LocalTypography provides typography,LocalMotion provides ValueAnimator.areAnimatorsEnabled(),content=content)
}
