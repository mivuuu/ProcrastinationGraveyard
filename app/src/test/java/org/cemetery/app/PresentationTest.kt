package org.cemetery.app
import org.cemetery.app.feature.cemetery.*
import org.junit.Assert.*
import org.junit.Test
class PresentationTest {
    @Test fun selectionStartsWithOriginalTransform(){val p=GravePresentation();assertTrue(p.select("g"));assertEquals("g",p.id);assertEquals(PresentationStage.FLYING,p.stage);assertEquals(0f,p.travel);assertEquals(0f,p.yaw);assertFalse(p.informationReady)}
    @Test fun spinShowsEverySideBeforeInformation(){val p=GravePresentation();p.select("g");val angles=mutableListOf<Float>();repeat(135){p.advance(.01f);angles+=p.yaw;if(it<130)assertFalse(p.informationReady)};p.advance(.02f);assertTrue(angles.any{it in 80f..100f});assertTrue(angles.any{it in 170f..190f});assertTrue(angles.any{it in 260f..280f});assertEquals(360f,p.yaw);assertEquals(1f,p.travel);assertTrue(p.informationReady)}
    @Test fun stopsAfterExactlyOneRevolution(){val p=GravePresentation();p.select("g");p.advance(2f);p.advance(20f);assertEquals(360f,p.yaw);assertEquals(PresentationStage.INSPECTING,p.stage)}
    @Test fun rapidSecondSelectionIsIgnored(){val p=GravePresentation();p.select("first");assertFalse(p.select("second"));assertEquals("first",p.id)}
    @Test fun dismissalReturnsExactlyToOriginalTransform(){val p=GravePresentation();p.select("g");p.advance(2f);p.dismiss();assertEquals(PresentationStage.RETURNING,p.stage);p.advance(.24f);assertTrue(p.travel in .1f.. .9f);p.advance(.25f);assertFalse(p.busy);assertNull(p.id);assertEquals(0f,p.travel);assertEquals(0f,p.yaw)}
    @Test fun cancellingMidSpinReturnsSafely(){val p=GravePresentation();p.select("g");p.advance(.4f);val travel=p.travel;p.dismiss();p.advance(.1f);assertTrue(p.travel<travel);p.advance(1f);assertFalse(p.busy);assertTrue(p.select("next"))}
    @Test fun repeatedDismissDoesNotRestartReturn(){val p=GravePresentation();p.select("g");p.advance(2f);p.dismiss();p.advance(.3f);p.dismiss();p.advance(.2f);assertFalse(p.busy)}
    @Test fun reducedMotionKeepsInformationAndCleanup(){val p=GravePresentation();p.select("g",false);assertTrue(p.informationReady);p.dismiss(false);assertFalse(p.busy)}
    @Test fun invalidTimeCannotCorruptState(){val p=GravePresentation();p.select("g");p.advance(Float.NaN);p.advance(-1f);assertEquals(0f,p.travel)}
    @Test fun lifecycleResetReleasesSelection(){val p=GravePresentation();p.select("g");p.advance(.6f);p.reset();assertFalse(p.busy);assertNull(p.id)}
    @Test fun originalPoseDoesNotDependOnViewport(){val p=PresentationFraming.pose(5f,-9f,1.9f,2f,12f,21f,30f,32f,.6f,0f,0f);assertEquals(5f,p.x);assertEquals(-9f,p.z);assertEquals(0f,p.y);assertEquals(1f,p.scale)}
    @Test fun foregroundRemainsInFrontOfNearPlaneAtEveryZoom(){for(focal in listOf(25f,32f,44f))for(aspect in listOf(.45f,.8f,2.2f)){val p=PresentationFraming.pose(200f,-9f,1.9f,2f,12f,21f,30f,focal,aspect,1f,360f);assertEquals(2f,p.x);assertTrue(p.y>0);assertTrue(p.z<21f && p.z>10f);assertTrue(p.scale>0);assertEquals(360f,p.yaw)}}
    @Test fun allStoneSilhouettesHaveVolumeAndDistinctProfiles(){val profiles=(0..3).map(StoneProfiles::outline);assertEquals(4,profiles.distinct().size);profiles.forEach{p->assertTrue(p.size>20);assertTrue(p.minOf{it.first}<-.5f);assertTrue(p.maxOf{it.second}>1.4f)}}
}
