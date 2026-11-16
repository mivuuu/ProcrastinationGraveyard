package org.cemetery.app

import org.junit.Assert.*
import org.junit.Test
import org.cemetery.app.ui.calendarCells
import org.cemetery.app.core.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.*
import java.security.MessageDigest

class CustomDesignTest {
    @Test fun calendarsRespectLeapYearsAndLocalWeekStarts(){
        val month=YearMonth.of(2028,2)
        for(first in DayOfWeek.entries){
            val cells=calendarCells(month,first)
            assertEquals(0,cells.size%7)
            assertEquals((1..29).map{month.atDay(it)},cells.filterNotNull())
            assertEquals(first,cells.firstNotNullOf{it}.minusDays(cells.indexOfFirst{it!=null}.toLong()).dayOfWeek)
        }
    }
    @Test fun calendarSupportsYearBoundariesAndSixWeekMonths(){
        for(month in listOf(YearMonth.of(2026,11),YearMonth.of(2026,12),YearMonth.of(2027,1),YearMonth.of(2026,2))){
            val cells=calendarCells(month,DayOfWeek.MONDAY)
            assertTrue(cells.size in listOf(28,35,42))
            assertEquals(month.lengthOfMonth(),cells.filterNotNull().distinct().size)
        }
    }
    @Test fun bothPalettesMeetNormalTextContrast(){
        fun contrast(a:Color,b:Color):Float {val x=a.luminance();val y=b.luminance();return (maxOf(x,y)+.05f)/(minOf(x,y)+.05f)}
        for(c in listOf(LightCemeteryColors,DarkCemeteryColors)){
            for(background in listOf(c.background,c.backgroundEnd,c.surface,c.elevated,c.navigation,c.selected)){
                assertTrue("Primary text contrast",contrast(c.text,background)>=4.5f)
                // Selected controls use primary text; secondary labels use their regular surfaces.
                if(background!=c.selected)assertTrue("Secondary text contrast",contrast(c.secondary,background)>=4.5f)
            }
            assertTrue(contrast(c.onAccent,c.accent)>=4.5f)
            assertTrue(contrast(c.onAccent,c.accentEnd)>=4.5f)
            assertTrue(contrast(c.error,c.errorSurface)>=4.5f)
        }
    }
    @Test fun visibleUiDoesNotImportMaterialComponents(){
        File("src/main/java").walkTopDown().filter{it.extension=="kt"}.forEach { source ->
            assertFalse("Material UI imported in $source",Regex("import androidx\\.compose\\.material").containsMatchIn(source.readText()))
        }
    }
    @Test fun rendererAndNativeAssetsRemainUnchanged(){
        val root=File("..");val manifest=File(root,"docs/verification/ui-renderer-before.sha256")
        manifest.readLines().filter{it.isNotBlank()}.forEach { line ->
            val (expected,path)=line.split(Regex("\\s+"),limit=2)
            val actual=MessageDigest.getInstance("SHA-256").digest(File(root,path).readBytes()).joinToString(""){"%02x".format(it)}
            assertEquals("Native renderer changed: $path",expected,actual)
        }
    }
    @Test fun locallyBundledFontCoversEnglishAndRussian(){
        // Inspect the actual TrueType Unicode cmap, including glyph-zero exclusions.
        val b=ByteBuffer.wrap(File("src/main/res/font/nunito.ttf").readBytes()).order(ByteOrder.BIG_ENDIAN)
        fun u16(at:Int)=b.getShort(at).toInt() and 65535
        val tables=u16(4)
        val cmap=(0 until tables).map{12+16*it}.first{b.getInt(it)==0x636d6170}.let{b.getInt(it+8)}
        val offsets=(0 until u16(cmap+2)).map{cmap+4+it*8}.filter{u16(it)==0 || u16(it)==3}.map{cmap+b.getInt(it+4)}
        fun glyph(code:Int):Int {
            for(base in offsets){
                when(u16(base)){
                    12->{for(i in 0 until b.getInt(base+12)){val group=base+16+i*12;val start=b.getInt(group);val end=b.getInt(group+4);if(code in start..end)return b.getInt(group+8)+code-start}}
                    4->{val n=u16(base+6)/2;for(i in 0 until n){val end=u16(base+14+i*2);val start=u16(base+16+2*n+i*2);if(code in start..end){val delta=b.getShort(base+16+4*n+i*2).toInt();val rangeAt=base+16+6*n+i*2;val range=u16(rangeAt);return if(range==0)(code+delta) and 65535 else u16(rangeAt+range+(code-start)*2).let{if(it==0)0 else (it+delta) and 65535}}}}
                }
            };return 0
        }
        val letters=('A'..'Z')+('a'..'z')+('А'..'я')+listOf('Ё','ё')
        for(letter in letters)assertTrue("Missing bundled glyph $letter",glyph(letter.code)!=0)
        assertTrue(File("src/main/assets/fonts/Nunito-OFL.txt").readText().contains("SIL OPEN FONT LICENSE Version 1.1"))
    }
}
