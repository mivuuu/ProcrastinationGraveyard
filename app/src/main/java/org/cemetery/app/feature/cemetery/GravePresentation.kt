package org.cemetery.app.feature.cemetery

import kotlin.math.*

enum class PresentationStage { IDLE, FLYING, INSPECTING, RETURNING }

/** Transient rendering state only. Never changes a grave's stored coordinates or the camera. */
class GravePresentation {
    var id: String?=null; private set
    var stage=PresentationStage.IDLE; private set
    var travel=0f; private set
    var yaw=0f; private set
    private var elapsed=0f
    private var returnTravel=0f
    private var returnYaw=0f
    val busy get()=stage!=PresentationStage.IDLE
    val informationReady get()=stage==PresentationStage.INSPECTING

    fun select(graveId: String, animated: Boolean=true): Boolean {
        if(busy)return false
        id=graveId;elapsed=0f;travel=if(animated)0f else 1f;yaw=if(animated)0f else 360f
        stage=if(animated)PresentationStage.FLYING else PresentationStage.INSPECTING
        return true
    }
    fun dismiss(animated: Boolean=true) {
        if(stage==PresentationStage.IDLE || stage==PresentationStage.RETURNING)return
        if(!animated){reset();return}
        returnTravel=travel;returnYaw=yaw;elapsed=0f;stage=PresentationStage.RETURNING
    }
    fun advance(seconds: Float) {
        if(!seconds.isFinite() || seconds<=0)return
        elapsed+=seconds
        when(stage) {
            PresentationStage.FLYING -> {
                travel=ease((elapsed/FLY_SECONDS).coerceIn(0f,1f))
                yaw=360f*ease((elapsed/SPIN_SECONDS).coerceIn(0f,1f))
                if(elapsed>=SPIN_SECONDS){travel=1f;yaw=360f;stage=PresentationStage.INSPECTING}
            }
            PresentationStage.RETURNING -> {
                val t=ease((elapsed/RETURN_SECONDS).coerceIn(0f,1f))
                travel=returnTravel*(1-t);yaw=returnYaw+(360f-returnYaw)*t
                if(elapsed>=RETURN_SECONDS)reset()
            }
            else -> Unit
        }
    }
    fun reset(){id=null;stage=PresentationStage.IDLE;travel=0f;yaw=0f;elapsed=0f}
    companion object {
        const val FLY_SECONDS=.95f
        const val SPIN_SECONDS=1.35f
        const val RETURN_SECONDS=.48f
        private fun ease(t: Float)=t*t*(3-2*t)
    }
}

internal data class PresentationPose(val x: Float,val y: Float,val z: Float,val scale: Float,val yaw: Float)
internal object PresentationFraming {
    /** Size/position are derived from perspective geometry, not hardcoded screen pixels. */
    fun pose(x: Float,z: Float,modelHeight: Float,cameraX: Float,cameraHeight: Float,cameraDepth: Float,
             lookDepth: Float,focal: Float,aspect: Float,travel: Float,yaw: Float,availableFraction: Float=.47f): PresentationPose {
        val length=hypot(cameraHeight,lookDepth)
        val forwardY=-cameraHeight/length;val forwardZ=-lookDepth/length
        val upY=lookDepth/length;val upZ=-cameraHeight/length
        val distance=7f
        val halfHeight=distance*12f/focal
        // Portrait: above the capped task sheet. Wide screens use a smaller presentation.
        val heightFraction=minOf(.30f,availableFraction*.72f,aspect*.75f)
        val targetScale=heightFraction*2*halfHeight/modelHeight
        val vertical=(1-availableFraction)*halfHeight
        val targetY=cameraHeight+forwardY*distance+upY*vertical-modelHeight*targetScale/2
        val targetZ=cameraDepth+forwardZ*distance+upZ*vertical
        return PresentationPose(x+(cameraX-x)*travel,targetY*travel,z+(targetZ-z)*travel,
            1+(targetScale-1)*travel,yaw)
    }
}
