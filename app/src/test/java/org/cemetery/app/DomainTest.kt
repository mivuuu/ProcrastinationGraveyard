package org.cemetery.app
import org.junit.Assert.*
import org.junit.Test
import org.cemetery.app.domain.*
import java.time.*
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.random.Random
class DomainTest {
    private val task=Task("id","A commitment","",0,100_000,100_000)
    @Test fun creationValidation(){assertTrue(LifecycleRules.validate("Small step"));assertFalse(LifecycleRules.validate("  "));assertFalse(LifecycleRules.validate("a".repeat(201)))}
    @Test fun activeBeforeApproach(){assertEquals(TaskStatus.ACTIVE,task.status(1))}
    @Test fun approachingUsesDeterministicQuarter(){assertEquals(TaskStatus.APPROACHING,task.status(75_000));assertEquals(TaskStatus.ACTIVE,task.status(74_999))}
    @Test fun exactDeadlineIsNotOverdue(){assertEquals(TaskStatus.APPROACHING,task.status(100_000));assertFalse(LifecycleRules.missed(task,100_000))}
    @Test fun oneMillisecondLateIsOverdue(){assertEquals(TaskStatus.OVERDUE,task.status(100_001));assertTrue(LifecycleRules.missed(task,100_001))}
    @Test fun completedOnTimeNeverCreatesGrave(){val done=task.copy(completedAt=99_999);assertEquals(TaskStatus.COMPLETED,done.status(Long.MAX_VALUE));assertFalse(LifecycleRules.missed(done,Long.MAX_VALUE))}
    @Test fun exactDeadlineCompletionIsOnTime(){assertEquals(TaskStatus.COMPLETED,task.copy(completedAt=100_000).status(200_000))}
    @Test fun lateCompletionIsResurrected(){val done=task.copy(completedAt=100_001);assertEquals(TaskStatus.RESURRECTED,done.status(200_000));assertTrue(LifecycleRules.missed(done,200_000))}
    @Test fun reschedulePreservesOriginalAndChangesGraveKey(){val next=task.copy(deadline=200_000);assertEquals(100_000L,next.originalDeadline);assertNotEquals(LifecycleRules.graveKey(task),LifecycleRules.graveKey(next));assertEquals(TaskStatus.ACTIVE,next.status(100_001))}
    @Test fun healthIsBounded(){assertEquals(1f,task.health(-1));assertEquals(0f,task.health(200_000));assertTrue(task.health(50_000) in .49f.. .51f)}
    @Test fun coordinatesAreDeterministic(){for(i in 0..5000)assertEquals(CemeteryPlacement.plot(i),CemeteryPlacement.plot(i))}
    @Test fun plotsOccupyFourRecognizableBands(){for(i in 0..5000){val p=CemeteryPlacement.plot(i);val row=i%4;assertEquals(CemeteryPlacement.FRONT_Z-row*CemeteryPlacement.ROW_SPACING,p.z,.251f);assertEquals(CemeteryPlacement.sectionAt(p.x),p.chunk)}}
    @Test fun plotsNeverCollide(){val points=(0..1000).map(CemeteryPlacement::plot);for(i in points.indices)for(j in i+1 until points.size)assertTrue(hypot(points[i].x-points[j].x,points[i].z-points[j].z)>=CemeteryPlacement.SPACING)}
    @Test fun expansionIsHorizontalInBothDirections(){val plots=(0..500).map(CemeteryPlacement::plot);assertTrue(plots.any{it.x>100});assertTrue(plots.any{it.x< -100});assertTrue(plots.all{it.z> -10f && it.z<3f})}
    @Test fun onlyNearbyHorizontalSectionsRequested(){assertEquals(-2..2,CemeteryPlacement.nearby(0f));assertEquals(98..102,CemeteryPlacement.nearby(1440f));assertEquals(-102.. -98,CemeteryPlacement.nearby(-1440f))}
    @Test fun firstFourGravesPopulateAllDepthRows(){val first=(0..3).map(CemeteryPlacement::plot);assertEquals(4,first.map{((CemeteryPlacement.FRONT_Z-it.z)/CemeteryPlacement.ROW_SPACING).roundToInt()}.distinct().size);assertTrue(first.all{kotlin.math.abs(it.x)<1f})}
    @Test fun epitaphAvoidsRecentIdentifiers(){val recent=(0..11).toList();repeat(100){assertFalse(EpitaphPicker.choose(recent,Random(it)) in recent)}}
    @Test fun epitaphLibraryExhaustionHasCandidates(){assertTrue(EpitaphPicker.choose((0..23).toList(),Random(4)) in 0..23)}
    @Test fun timestampIndependentOfDisplayZone(){val instant=Instant.parse("2026-10-25T01:30:00Z");assertEquals(instant,instant.atZone(ZoneId.of("Europe/Berlin")).toInstant());assertEquals(instant,instant.atZone(ZoneId.of("Asia/Tokyo")).toInstant())}
    @Test fun daylightSavingGapIsResolvedByZone(){val local=LocalDateTime.of(2026,3,29,2,30);assertEquals(3,local.atZone(ZoneId.of("Europe/Berlin")).hour)}
    @Test fun invalidOrdinalRejected(){try{CemeteryPlacement.plot(-1);fail()}catch(_:IllegalArgumentException){}}
}
