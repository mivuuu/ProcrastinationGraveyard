package org.cemetery.app.feature.cemetery

import com.google.android.filament.Engine
import com.google.android.filament.VertexBuffer
import io.github.sceneview.geometries.*
import io.github.sceneview.math.*
import io.github.sceneview.safeDestroyGeometry
import kotlin.math.*

internal data class CartoonPart(val mesh:Geometry,val position:Position,val size:Scale,val material:Int,val id:String?=null,var rotation:Rotation=Rotation(0f),var name:String="decoration")

private class CartoonBuilder : Geometry.Builder() {
    override fun build(engine: Engine): Geometry {
        vertexBuilder.normalized(VertexBuffer.VertexAttribute.TANGENTS,false)
        return super.build(engine)
    }
}
/** All sections and graves reuse these immutable meshes. No downloaded models/textures. */
internal class CartoonMeshFactory(private val engine: Engine) {
    private data class Source(val vertices:List<Geometry.Vertex>,val indices:List<Int>)
    private val sources=mutableMapOf<Geometry,Source>()
    private val models=mutableMapOf<String,Geometry>()
    private fun create(vertices:List<Geometry.Vertex>,indices:List<List<Int>>,source:Boolean=true)=
        CartoonBuilder().vertices(vertices).primitivesIndices(indices).build(engine).also{if(source)sources[it]=Source(vertices,indices.flatten())}
    val cube=create(Cube.getVertices(Size(1f),Position(0f)),Cube.INDICES)
    val sphere=create(Sphere.getVertices(1f,Position(0f),8,16),Sphere.getIndices(8,16))
    val ground=extrude(listOf(-.5f to -.5f,.5f to -.5f,.5f to .5f,-.5f to .5f),.001f,false)
    val triangle=extrude(listOf(-.5f to 0f,.5f to 0f,0f to 1f),.16f,false)
    val cloud=extrude(listOf(-.43f to 0f,.43f to 0f)+(1..47).map { i ->
        val x=.43f-i*.86f/48
        val lobes=listOf(Triple(-.26f,.10f,.17f),Triple(-.03f,.17f,.29f),Triple(.25f,.10f,.18f))
        x to lobes.maxOf { (cx,cy,r) -> if(abs(x-cx)<=r) cy+sqrt((r*r-(x-cx)*(x-cx)).coerceAtLeast(0f)) else 0f }
    },.025f,false)
    val patch=extrude((0 until 48).map { i -> val a=i*2*PI.toFloat()/48;val r=.5f*(1+.09f*sin(a*3)+.06f*cos(a*5));cos(a)*r to sin(a)*r },.001f,false)
    val heads=(0..3).map { extrude(StoneProfiles.outline(it),.46f,true) }

    private fun extrude(profile: List<Pair<Float,Float>>, depth: Float, bevel: Boolean): Geometry {
        val vertices=mutableListOf<Geometry.Vertex>()
        val indices=mutableListOf<Int>()
        fun triangle(a: Position,b: Position,c: Position) {
            val u=b-a;val v=c-a
            val nx=u.y*v.z-u.z*v.y;val ny=u.z*v.x-u.x*v.z;val nz=u.x*v.y-u.y*v.x
            val length=sqrt(nx*nx+ny*ny+nz*nz).coerceAtLeast(.000001f)
            val normal=Direction(nx/length,ny/length,nz/length)
            listOf(a,b,c).forEach { indices+=vertices.size;vertices+=Geometry.Vertex(it,normal) }
        }
        fun quad(a: Position,b: Position,c: Position,d: Position) { triangle(a,b,c);triangle(a,c,d) }
        val cy=(profile.maxOf{it.second}+profile.minOf{it.second})/2
        val outer=depth/2*(if(bevel).65f else 1f)
        fun point(p: Pair<Float,Float>,z: Float, inset: Boolean=false)=Position(p.first*(if(inset).92f else 1f),cy+(p.second-cy)*(if(inset).95f else 1f),z)
        profile.indices.forEach { i ->
            val a=profile[i];val b=profile[(i+1)%profile.size]
            triangle(Position(0f,cy,depth/2),point(a,depth/2,bevel),point(b,depth/2,bevel))
            triangle(Position(0f,cy,-depth/2),point(b,-depth/2,bevel),point(a,-depth/2,bevel))
            quad(point(a,outer),point(a,-outer),point(b,-outer),point(b,outer))
            if(bevel) {
                quad(point(a,depth/2,true),point(a,outer),point(b,outer),point(b,depth/2,true))
                quad(point(a,-outer),point(a,-depth/2,true),point(b,-depth/2,true),point(b,-outer))
            }
        }
        return create(vertices,listOf(indices))
    }
    /** Static batches are owned by a section; the small set of grave/outline batches is reused. */
    fun batch(parts:List<CartoonPart>,cacheKey:String?):Geometry {
        cacheKey?.let{models[it]?.let{return it}}
        val vertices=mutableListOf<Geometry.Vertex>();val indices=mutableListOf<Int>()
        parts.forEach{part->
            val source=checkNotNull(sources[part.mesh]);val offset=vertices.size
            val ax=part.rotation.x*PI.toFloat()/180;val ay=part.rotation.y*PI.toFloat()/180;val az=part.rotation.z*PI.toFloat()/180
            val cx=cos(ax);val sx=sin(ax);val cy=cos(ay);val sy=sin(ay);val cz=cos(az);val sz=sin(az)
            fun rotate(x:Float,y:Float,z:Float):Position {
                val y1=y*cx-z*sx;val z1=y*sx+z*cx
                val x2=x*cy+z1*sy;val z2=-x*sy+z1*cy
                return Position(x2*cz-y1*sz,x2*sz+y1*cz,z2)
            }
            source.vertices.forEach{v->
                val pos=rotate(v.position.x*part.size.x,v.position.y*part.size.y,v.position.z*part.size.z)+part.position
                val n=v.normal?:Direction(0f,1f,0f)
                val normal=rotate(n.x/part.size.x,n.y/part.size.y,n.z/part.size.z)
                val length=sqrt(normal.x*normal.x+normal.y*normal.y+normal.z*normal.z).coerceAtLeast(.00001f)
                vertices+=Geometry.Vertex(pos,normal/length)
            }
            indices+=source.indices.map{it+offset}
        }
        return create(vertices,listOf(indices),false).also{if(cacheKey!=null)models[cacheKey]=it}
    }
    fun close() { (listOf(cube,sphere,ground,triangle,cloud,patch)+heads+models.values).forEach{engine.safeDestroyGeometry(it)};models.clear();sources.clear() }
}
