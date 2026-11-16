package org.cemetery.app.feature.cemetery

import androidx.compose.runtime.saveable.listSaver
import org.cemetery.app.domain.CemeteryPlacement
import org.cemetery.app.domain.Grave
import kotlin.math.*

/** Normal navigation changes X and lens only. Presentation freezes the exact current frame. */
class CameraController {
    var x=0f; private set
    var focalLength=DEFAULT_FOCAL; private set
    var targetX=x; private set
    var targetFocalLength=focalLength; private set
    var locked=false; private set
    private var minimumX=-CemeteryPlacement.SECTION_WIDTH
    private var maximumX=CemeteryPlacement.SECTION_WIDTH
    private var initialized=false
    fun configure(graves: List<Grave>) {
        minimumX=(graves.minOfOrNull{it.x}?:0f)-CemeteryPlacement.SECTION_WIDTH
        maximumX=(graves.maxOfOrNull{it.x}?:0f)+CemeteryPlacement.SECTION_WIDTH
        if(!initialized && graves.isNotEmpty() && !locked){x=graves.minBy{abs(it.x)}.x;targetX=x;initialized=true}
    }
    fun panBy(distance: Float) {
        if(locked || !distance.isFinite())return
        initialized=true;targetX=(targetX+distance).coerceIn(minimumX,maximumX)
    }
    fun zoomBy(ratio: Float) {
        if(locked || !ratio.isFinite() || ratio<=0)return
        initialized=true;targetFocalLength=(targetFocalLength*ratio).coerceIn(MIN_FOCAL,MAX_FOCAL)
    }
    fun lockForPresentation() { locked=true;initialized=true;targetX=x;targetFocalLength=focalLength }
    fun unlock() { locked=false }
    fun advance(seconds: Float) {
        if(locked)return
        val alpha=1-exp(-9f*seconds.coerceIn(0f,.1f))
        x+=(targetX-x)*alpha;focalLength+=(targetFocalLength-focalLength)*alpha
    }
    fun saveState(): List<Any> = listOf("cartoon-v1",x,focalLength,targetX,targetFocalLength,initialized)
    companion object {
        const val HEIGHT=12f
        const val DEPTH=21f
        const val OFFSET_X=0f
        const val LOOK_DEPTH=30f
        const val MIN_FOCAL=25f
        const val DEFAULT_FOCAL=32f
        const val MAX_FOCAL=44f
        fun restoreState(values: List<Any>)=CameraController().apply {
            if(values.firstOrNull()=="cartoon-v1" && values.size==6) {
                x=values[1] as Float;focalLength=values[2] as Float
                targetX=values[3] as Float;targetFocalLength=values[4] as Float;initialized=values[5] as Boolean
            } else if(values.size>=11) {
                // Recover the previous navigation frame from the old camera-focus save format.
                val previous=values[8] as Float
                x=if(previous.isFinite())previous else values[0] as Float
                focalLength=(if(previous.isFinite())values[9] as Float else values[1] as Float).coerceIn(MIN_FOCAL,MAX_FOCAL)
                targetX=x;targetFocalLength=focalLength;initialized=values[6] as Boolean
            }
        }
        val Saver=listSaver<CameraController,Any>(save={it.saveState()},restore=::restoreState)
    }
}
