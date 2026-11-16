package org.cemetery.app.feature.cemetery

/** Texture updates acknowledge a real consumed frame. Ignore completion callbacks belonging to a previous pause, tab or released surface. */
internal class SurfaceFrameGate(private val changed:(Boolean)->Unit):AutoCloseable {
    private var generation=0
    private var active=true
    private var pending=false
    var ready=false;private set
    var probes=0;private set
    fun reset(){generation++;pending=false;ready=false;changed(false)}
    fun afterFrame(surface:CemeteryTextureView){
        if(!active || ready || pending)return
        pending=true;probes++
        val activation=generation
        surface.whenFrameRendered {
            if(active && activation==generation){pending=false;ready=true;changed(true)}
        }
    }
    override fun close(){active=false;generation++;pending=false;ready=false}
}
internal data class CemeteryViewportState(val session:CemeteryRenderSession,val gate:SurfaceFrameGate)
