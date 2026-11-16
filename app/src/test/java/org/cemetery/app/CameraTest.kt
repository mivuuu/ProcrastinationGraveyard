package org.cemetery.app
import org.cemetery.app.feature.cemetery.*
import org.cemetery.app.domain.*
import org.junit.Assert.*
import org.junit.Test
class CameraTest {
    @Test fun horizontalMovementIsBounded(){val c=CameraController();c.panBy(1000f);assertEquals(CemeteryPlacement.SECTION_WIDTH,c.targetX);c.panBy(-2000f);assertEquals(-CemeteryPlacement.SECTION_WIDTH,c.targetX)}
    @Test fun opticalZoomIsBoundedAndNeverPans(){val c=CameraController();c.panBy(3f);c.zoomBy(100f);assertEquals(CameraController.MAX_FOCAL,c.targetFocalLength);assertEquals(3f,c.targetX);c.zoomBy(.0001f);assertEquals(CameraController.MIN_FOCAL,c.targetFocalLength);assertEquals(3f,c.targetX)}
    @Test fun selectionFreezesExactPositionAndZoom(){val c=CameraController();c.panBy(3f);c.zoomBy(1.2f);c.advance(.1f);val x=c.x;val zoom=c.focalLength;c.lockForPresentation();c.panBy(20f);c.zoomBy(.5f);repeat(150){c.advance(1f/60)};assertEquals(x,c.x);assertEquals(zoom,c.focalLength);assertEquals(x,c.targetX);assertEquals(zoom,c.targetFocalLength);c.unlock();c.panBy(1f);c.advance(.1f);assertTrue(c.x>x)}
    @Test fun frozenFramingSurvivesSavedState(){val c=CameraController();c.panBy(-4f);c.zoomBy(1.15f);repeat(150){c.advance(1f/60)};c.lockForPresentation();val restored=CameraController.restoreState(c.saveState());assertEquals(c.x,restored.x);assertEquals(c.focalLength,restored.focalLength);assertFalse(restored.locked);restored.advance(.1f);assertEquals(c.x,restored.x)}
    @Test fun smoothMovementAndZoomConverge(){val c=CameraController();c.panBy(3f);c.zoomBy(1.3f);c.advance(1f/60);assertTrue(c.x>0 && c.x<3);assertTrue(c.focalLength>32 && c.focalLength<c.targetFocalLength);repeat(150){c.advance(1f/60)};assertEquals(3f,c.x,.001f);assertEquals(c.targetFocalLength,c.focalLength,.001f)}
    @Test fun legacyWalkthroughStateIsSafelyDiscarded(){val c=CameraController.restoreState(listOf(4f,-200f));assertEquals(0f,c.x);assertEquals(CameraController.DEFAULT_FOCAL,c.focalLength)}
    @Test fun invalidGestureValuesAreIgnored(){val c=CameraController();c.panBy(Float.NaN);c.zoomBy(Float.POSITIVE_INFINITY);c.zoomBy(-1f);assertEquals(0f,c.targetX);assertEquals(32f,c.targetFocalLength)}
    @Test fun verticalDragDoesNotPanOrSelect(){val c=CameraController();val g=CemeteryGestures(c,8f,1f);g.down(50f,50f);g.move(51f,200f);g.move(90f,300f);assertEquals(0f,c.targetX);assertFalse(g.up(90f,300f))}
    @Test fun horizontalDragDoesNotSelect(){val c=CameraController();val g=CemeteryGestures(c,8f,1f);g.down(100f,100f);g.move(0f,105f);assertTrue(c.targetX>0);assertFalse(g.up(0f,105f))}
    @Test fun pinchChangesOnlyLensAndCannotBecomeATap(){val c=CameraController();val g=CemeteryGestures(c,8f,1f);g.down(50f,100f);g.secondPointer(100f);g.pinch(120f);g.pinch(150f);assertTrue(c.targetFocalLength>32f);assertEquals(0f,c.targetX);g.pointerUp();g.move(-100f,100f);assertEquals(0f,c.targetX);assertFalse(g.up(-100f,100f))}
    @Test fun smallFingerJitterIsATap(){val c=CameraController();val g=CemeteryGestures(c,8f,1f);g.down(10f,10f);g.move(12f,13f);assertEquals(0f,c.targetX);assertTrue(g.up(12f,13f))}
    @Test fun oldFocusSaveStateRestoresNavigationFrame(){val c=CameraController.restoreState(listOf(20f,40f,.4f,21f,42f,.5f,true,"g",-7f,31f,0f));assertEquals(-7f,c.x);assertEquals(31f,c.focalLength);assertFalse(c.locked)}
    @Test fun subtleFrontalPitchAndAllFourRowsFitAtMaximumZoom(){
        val before=kotlin.math.atan2(12.0,30.0)*180/kotlin.math.PI
        val after=kotlin.math.atan2(CameraController.HEIGHT.toDouble(),CameraController.LOOK_DEPTH.toDouble())*180/kotlin.math.PI
        assertTrue("Pitch reduction must remain subtle: ${before-after}",before-after in 5.0..8.0)
        val halfFov=kotlin.math.atan(12.0/CameraController.MAX_FOCAL)
        repeat(4){row->
            val z=org.cemetery.app.domain.CemeteryPlacement.FRONT_Z-row*org.cemetery.app.domain.CemeteryPlacement.ROW_SPACING
            val angle=kotlin.math.atan2(CameraController.HEIGHT-.85,CameraController.DEPTH-z.toDouble())
            assertTrue("Row $row is outside the maximum-zoom vertical viewport",kotlin.math.abs(angle-after*kotlin.math.PI/180)<halfFov)
        }
    }
}
