package org.cemetery.app.feature.cemetery

import dev.romainguy.kotlin.math.Quaternion
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import io.github.sceneview.geometries.Geometry
import io.github.sceneview.geometries.geometry
import io.github.sceneview.safeDestroyGeometry
import io.github.sceneview.math.*
import io.github.sceneview.node.*
import org.cemetery.app.domain.*
import kotlin.math.*
import kotlin.random.Random

internal data class StoneSpec(val id: String,val x: Float,val z: Float,val variant: Int,val flower: Boolean) {
    constructor(g: Grave):this(g.id,g.x,g.z,g.variant,g.resurrected)
    val height get()=(if(flower)1.28f else when(variant.mod(4)){1->1.72f;2->1.42f;3->1.69f;else->1.70f})*size
    val size get()=1.10f+((id.hashCode().toLong() and 0x7fffffff)%9)/100f
}
internal data class SproutSpec(val id: String,val health: Int)
private data class Built(val nodes: List<Node>,val hits: Map<Node,String>,val owned:List<Geometry> = emptyList(),val engine:Engine?=null) {
    val roots=nodes.filter{it.parent==null}
    fun destroy(){nodes.asReversed().forEach{runCatching{it.destroy()}};engine?.let{e->owned.forEach{e.safeDestroyGeometry(it)}}}
}

/** Scene graph resources are bounded by horizontal sections. A selected root is temporarily pinned. */
internal class DioramaWorld(private val engine: Engine,private val meshes: CartoonMeshFactory,private val materials: List<MaterialInstance>) {
    private val sections=mutableMapOf<Int,Built>()
    private val stones=mutableMapOf<String,Pair<StoneSpec,Built>>()
    private var plantsKey=emptyList<SproutSpec>()
    private var plants=Built(emptyList(),emptyMap())
    private var selected: StoneSpec?=null
    private var selectionVisual=Built(emptyList(),emptyMap())
    private val retired=mutableListOf<Built>()
    private val sky=builder().apply{sky()}.built()
    var nodes: List<Node> = emptyList();private set
    val hits=mutableMapOf<Node,String>()
    private fun builder()=CartoonWorldBuilder(engine,meshes,materials)
    fun update(range: IntRange,graves: List<StoneSpec>,sprouts: List<SproutSpec>,selectedId: String?): Boolean {
        var changed=false
        sections.keys.filter{it !in range}.forEach{retired+=sections.remove(it)!!;changed=true}
        range.forEach{index->if(index !in sections){sections[index]=builder().apply{section(index)}.built();changed=true}}
        val ids=graves.mapTo(hashSetOf()){it.id}
        stones.keys.filter{it !in ids}.forEach{retired+=stones.remove(it)!!.second;changed=true}
        graves.forEach{g->if(stones[g.id]?.first!=g){stones.remove(g.id)?.let{retired+=it.second};stones[g.id]=g to builder().apply{grave(g)}.built();changed=true}}
        if(sprouts!=plantsKey){retired+=plants;plantsKey=sprouts;plants=builder().apply{sprouts.forEachIndexed{i,p->sprout(-5.6f+(i%8)*1.6f,4.4f+i/8*.8f,p.health/10f)}}.built();changed=true}
        val selection=graves.find{it.id==selectedId}
        if(selected!=selection){selectionVisual.roots.forEach{it.isVisible=false};retired+=selectionVisual;selected=selection;selectionVisual=builder().apply{selection?.let{selectionEffect(it)}}.built();changed=true}
        if(changed){
            nodes=buildList{addAll(sky.roots);sections.values.forEach{addAll(it.roots)};stones.values.forEach{addAll(it.second.roots)};addAll(plants.roots);addAll(selectionVisual.roots)}
            hits.clear();stones.values.forEach{hits.putAll(it.second.hits)}
        }
        return changed
    }
    fun transform(id: String,pose: PresentationPose) {
        val root=stones[id]?.second?.nodes?.firstOrNull()?:return
        root.transform=Transform(Position(pose.x,pose.y,pose.z),Quaternion.fromEuler(Rotation(0f,pose.yaw,0f)),Scale(pose.scale))
        if(selected?.id==id)selectionVisual.nodes.firstOrNull()?.let{it.isVisible=true;it.transform=root.transform}
    }
    fun restore(id: String) {
        stones[id]?.let{(g,_)->transform(id,PresentationPose(g.x,0f,g.z,1f,0f))}
        if(selected?.id==id)selectionVisual.roots.forEach{it.isVisible=false}
    }
    /** Only sky/clouds inherit the camera. World plots, foliage and presentation stay independent. */
    fun frameSky(cameraNode: CameraNode) {
        val root=sky.nodes.first()
        if(root.parent!==cameraNode){
            root.parent=cameraNode;root.position=Position(0f,0f,-100f);root.rotation=Rotation(0f)
        }
        // Use the actual lens projection, including viewport aspect, instead of a sensor-size guess.
        val projection=cameraNode.projectionTransform
        root.scale=Scale(100f/projection.x.x,100f/projection.y.y,1f)
    }
    fun releaseRetired(){retired.forEach{it.destroy()};retired.clear()}
    fun close(){stones.values.forEach{it.second.destroy()};sections.values.forEach{it.destroy()};plants.destroy();selectionVisual.destroy();sky.destroy();releaseRetired();nodes=emptyList();hits.clear()}
}

private class CartoonWorldBuilder(private val engine: Engine,private val meshes: CartoonMeshFactory,private val materials: List<MaterialInstance>) {
    private val nodes=mutableListOf<Node>()
    private val hits=mutableMapOf<Node,String>()
    private val parts=mutableListOf<CartoonPart>()
    private var parent:Node?=null
    private var unit=1f
    private var modelKey:String?=null
    fun built():Built {
        val owned=mutableListOf<Geometry>()
        parts.groupBy{it.material to it.id}.forEach{(key,items)->
            // Stone tones share topology; only their material instance differs.
            val category=if(key.first in 2..5)"stone" else key.first.toString()
            val cache=modelKey?.let{"$it:$category:${key.second!=null}"}
            val mesh=meshes.batch(items,cache)
            if(cache==null)owned+=mesh
            val node=RenderableNode(engine,primitiveCount=1,boundingBox=mesh.boundingBox,materialInstances=listOf(materials[key.first]),builder={geometry(mesh,listOf(0..mesh.primitivesOffsets.last().last));castShadows(false);receiveShadows(false)}).apply{
                scale=Scale(unit);isTouchable=key.second!=null;isHittable=key.second!=null
                name=items.firstOrNull{it.name.startsWith("forest:") || it.name=="cartoon-clouds"}?.name?:if(key.second!=null)"grave:${key.second}" else "decoration-batch"
                if(key.second==null)collisionShape=null
                parent=this@CartoonWorldBuilder.parent
            }
            nodes+=node;key.second?.let{hits[node]=it}
        }
        return Built(nodes,hits,owned,engine)
    }
    private fun group(name:String,x:Float=0f,z:Float=0f):Node=Node(engine).apply{
        this.name=name;position=Position(x,0f,z);isTouchable=false;isHittable=false;nodes+=this;this@CartoonWorldBuilder.parent=this
    }
    private fun shape(mesh:Geometry,x:Float,y:Float,z:Float,sx:Float,sy:Float,sz:Float,mat:Int,id:String?=null):CartoonPart =
        CartoonPart(mesh,Position(x,y,z),Scale(sx,sy,sz),mat,id).also{parts+=it}
    private fun box(x:Float,y:Float,z:Float,w:Float,h:Float,d:Float,mat:Int,id:String?=null)=shape(meshes.cube,x,y,z,w,h,d,mat,id)
    private fun ball(x:Float,y:Float,z:Float,rx:Float,ry:Float,rz:Float,mat:Int,id:String?=null)=shape(meshes.sphere,x,y,z,rx,ry,rz,mat,id)
    private fun tuft(x:Float,z:Float,size:Float=1f){
        repeat(3){i->shape(meshes.triangle,x+(i-1)*.13f*size,.02f,z,.16f*size,(if(i==1).50f else .34f)*size,.15f,8).rotation=Rotation(0f,0f,(1-i)*18f)}
    }
    private fun daisy(x:Float,z:Float,size:Float=1f){
        repeat(5){i->val a=i*2*PI.toFloat()/5;ball(x+cos(a)*.10f*size,.035f,z+sin(a)*.10f*size,.095f*size,.018f,.065f*size,6)}
        ball(x,.06f,z,.065f*size,.025f,.065f*size,11)
    }
    fun grave(g: StoneSpec) {
        group("miniature:${g.id}",g.x,g.z);unit=g.size;modelKey="grave:${g.variant.mod(4)}:${g.flower}"
        if(g.flower){flower(g.id);return}
        val mat=2+((g.id.hashCode().toLong() and 0x7fffffff)%4).toInt()
        shape(meshes.heads[g.variant.mod(4)],0f,.035f,0f,1f,1f,1f,mat,g.id)
        // Quiet front symbol; the plain back and shaded sides remain visible during the spin.
        ball(0f,1.03f,.245f,.23f,.24f,.018f,18,g.id)
        ball(-.085f,1.055f,.266f,.055f,.060f,.012f,mat,g.id)
        ball(.085f,1.055f,.266f,.055f,.060f,.012f,mat,g.id)
        repeat(3){i->box((i-1)*.10f,.81f,.25f,.07f,.11f,.035f,18,g.id)}
        repeat(3){i->ball((i-1)*.35f,.035f,.28f,.28f,.10f+(if(i==1).04f else 0f),.20f,19)}
        tuft(-.76f,.04f,.65f);tuft(.77f,.06f,.60f)
    }
    fun selectionEffect(g: StoneSpec) {
        unit=g.size
        if(g.flower){
            // A quiet, round pool of light beneath the flower, never a box/contour proxy.
            group("halo:${g.id}",g.x,g.z);modelKey="flower-halo"
            ball(0f,.012f,0f,.47f,.015f,.34f,21)
            ball(0f,.025f,0f,.37f,.012f,.26f,6)
            return
        }
        group("outline:${g.id}",g.x,g.z);modelKey="outline:${g.variant.mod(4)}:false"
        val points=StoneProfiles.outline(g.variant)
        for(z in listOf(.25f,-.25f)) (points+points.first()).zipWithNext().forEach{(a,b)->
            val dx=b.first-a.first;val dy=b.second-a.second
            box((a.first+b.first)/2,(a.second+b.second)/2+.035f,z,.020f,hypot(dx,dy)+.01f,.020f,6).apply{rotation=Rotation(0f,0f,-atan2(dx,dy)*180f/PI.toFloat());name="selection-outline"}
        }
    }
    private fun flower(id:String){
        box(0f,.48f,0f,.06f,.96f,.06f,9,id)
        ball(-.15f,.44f,0f,.23f,.07f,.10f,9,id);ball(.15f,.63f,0f,.23f,.07f,.10f,9,id)
        repeat(6){i->val a=i*PI.toFloat()/3;ball(cos(a)*.21f,1.03f+sin(a)*.21f,0f,.16f,.16f,.07f,10,id)}
        ball(0f,1.03f,.09f,.13f,.13f,.07f,11,id)
    }
    fun sprout(x:Float,z:Float,health:Float){val h=.32f+health*.30f;box(x,h/2,z,.045f,h,.045f,9);ball(x-.11f,h*.65f,z,.18f,.045f,.10f,9);ball(x+.11f,h*.8f,z,.18f,.045f,.10f,9)}
    fun section(index:Int){
        val random=Random(0xCA4700 xor index);val center=index*CemeteryPlacement.SECTION_WIDTH
        // A paper-thin continuous plane extends far beyond every camera ray in front.
        shape(meshes.ground,center,-.015f,85f,CemeteryPlacement.SECTION_WIDTH+.01f,212f,1f,0).rotation=Rotation(-90f,0f,0f)
        repeat(4){i->shape(meshes.patch,center+(random.nextFloat()-.5f)*14f,if(i%2==0).025f else .045f,6f-random.nextFloat()*20f,6f+random.nextFloat()*5f,4f+random.nextFloat()*4f,1f,if(i%2==0)1 else 20).rotation=Rotation(-90f,0f,0f)}
        // Three overlapping, flat-color evergreen layers, matching the triangular reference.
        repeat(3){layer->
            val z=-15.8f-layer*3.1f
            val count=7
            repeat(count){i->
                val x=center-7.2f+(i+.5f)*14.4f/count+(random.nextFloat()-.5f)*.65f
                val h=if(layer==0)3.1f+random.nextFloat()*1.1f else 3.4f+random.nextFloat()*1.1f
                val mat=12+layer
                val root=shape(meshes.triangle,x,.18f,z,2.8f,h,.8f,mat);root.name="forest:$index:$layer:$i"
                shape(meshes.triangle,x,h*.42f,z+.06f,2.0f,h*.72f,.8f,if(layer==0 && i%4==1)13 else mat)
                box(x,.35f,z,.12f,.7f,.12f,7)
            }
        }
        // Low scalloped shrubs conceal the distant join between grass and forest silhouettes.
        repeat(7){i->val x=center-7.2f+i*2.4f;ball(x,.2f,-15.1f,1.25f,.5f,.5f,17)}
        daisy(center-3.8f,5.4f,1.5f);daisy(center+2.7f,7.2f,1.5f)
        repeat(5){i->
            val x=center-6.2f+i*2.9f+(random.nextFloat()-.5f)*.7f
            val z=5.5f-random.nextFloat()*16f
            if(i%2==0)daisy(x,z,.85f) else tuft(x,z,.7f)
        }
    }
    fun sky(){
        group("cartoon-sky")
        // Vertical gradient strips sit behind all scene geometry; no fog or gray horizon.
        repeat(12){i->box(0f,-1.1f+i*.2f,-.5f,5f,.20f,.01f,22+i)}
        fun cloud(x:Float,y:Float,s:Float){shape(meshes.cloud,x,y,0f,.75f*s,.38f*s,1f,21).name="cartoon-clouds"}
        cloud(-.65f,.65f,1.1f);cloud(.82f,.60f,1.0f)
    }
}
