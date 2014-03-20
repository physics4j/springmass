/**
 * Copyright 2012 Tobias Gierke <tobias.gierke@code-sourcery.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.codesourcery.springmass.ui.opengl;

import java.io.IOException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

import de.codesourcery.springmass.simulation.Mass;
import de.codesourcery.springmass.simulation.SimulationParameters;
import de.codesourcery.springmass.simulation.SpringMassSystem;

public class ClothRenderer 
{
	private static final VertexAttributes POLYGON_VERTEX_ATTRIBUTES;
	
	static 
	{
		final VertexAttribute positionAttr = VertexAttribute.Position();		
		positionAttr.alias = "a_position";
		
		final VertexAttribute normalAttr = VertexAttribute.Normal();
		normalAttr.alias = "a_normal";	
		
		final VertexAttribute colorAttr = VertexAttribute.ColorUnpacked();
		colorAttr.alias = "a_color";			

		POLYGON_VERTEX_ATTRIBUTES = new VertexAttributes( positionAttr , normalAttr );
	}
	
	protected final FloatArrayBuilder floatArrayBuilder = new FloatArrayBuilder( 10240 , 10240 );
	
	protected DynamicVBO vbo;
	
	protected ShaderProgram polyProgram;	
	
	public ClothRenderer() throws IOException 
	{
		this.vbo = new DynamicVBO(false , 65536 , 7*4 );
		
		try {
			polyProgram = ShaderUtils.loadShader("default");
		} catch(Exception e) {
			throw new IOException("Failed to load 'default' shader: "+e.getMessage(),e);
		}		
	}

	public void renderCloth(Matrix4 modelMatrix,Matrix4 modelViewMatrix,Matrix4 modelViewProjectionMatrix,SpringMassSystem system,SimulationParameters parameters) 
	{
		final Mass[][] masses = system.getMassArray();
		
		final GL20 gl20 = Gdx.graphics.getGL20();

		gl20.glEnable( GL20.GL_DEPTH_TEST );
		gl20.glDisable( GL20.GL_CULL_FACE );

		final int rows = parameters.getGridRowCount();
		final int columns = parameters.getGridColumnCount();

		final boolean checkArea = parameters.getMaxSpringLength() > 0;
		final double maxLenSquared = parameters.getMaxSpringLength()*parameters.getMaxSpringLength();

		polyProgram.begin();

		polyProgram.setUniformMatrix("u_modelViewProjection" , modelViewProjectionMatrix );
		//shaderProgram.setUniformf( "diffuseColor" , new Vector3(1,0,0) );

		polyProgram.setUniformMatrix( "u_modelView" , modelViewMatrix );
		//shaderProgram.setUniformMatrix( "normalMatrix", camera. );		
		polyProgram.setUniformf( "vLightPos" , new Vector3(100,-300,-1000) );

		final Triangle t1 = new Triangle();
		final Triangle t2 = new Triangle();	    

		floatArrayBuilder.begin();
		
		// bind VBO
		vbo.bind( polyProgram , POLYGON_VERTEX_ATTRIBUTES );		
		
		int vertexCount = 0;
		for ( int y = 0 ; y < rows-1 ; y++) 
		{
			for ( int x = 0 ; x < columns-1 ; x++) 
			{
				Mass m0 = masses[x][y];
				Mass m1 = masses[x+1][y];
				Mass m2 = masses[x][y+1];
				Mass m3 = masses[x+1][y+1];

				t1.set(m0 , m2 , m1 ); // p0,p2,p1,m0.normal,m2.normal,m1.normal);
				t2.set(m1 , m2 , m3 ); // p1,p2,p3,m1.normal,m2.normal,m3.normal);	

				if ( checkArea ) 
				{
					if ( t1.noSideExceedsLengthSquared( maxLenSquared ) ) 
					{
						triangle( t1 );
						vertexCount +=3;
					}
					if ( t2.noSideExceedsLengthSquared( maxLenSquared ) ) {
						triangle( t2 );
						vertexCount +=3;
					}
				} else {
					triangle( t1 );
					triangle( t2 );
					vertexCount +=6;
				}
				
				if ( ( vertexCount +3 ) >= OpenGLRenderPanel.GL_DRAW_ARRAYS_MAX_VERTEX_COUNT ) { // JVM will dump core if array passed to glDrawArrays() contains too many vertices 
					final int bufferSize = floatArrayBuilder.end();
					vbo.setVertices( floatArrayBuilder.array , 0 , bufferSize );
					
					// render
					gl20.glDrawArrays(GL20.GL_TRIANGLES, 0 , vertexCount );
					floatArrayBuilder.begin();
					vertexCount = 0;
				}
			}
		}

		if ( vertexCount > 0 ) 
		{
			final int bufferSize = floatArrayBuilder.end();
			vbo.setVertices( floatArrayBuilder.array , 0 , bufferSize );
			
			// render
			gl20.glDrawArrays(GL20.GL_TRIANGLES, 0 , vertexCount );
		}
		
		polyProgram.end();
		
		vbo.unbind( polyProgram , POLYGON_VERTEX_ATTRIBUTES);
	}	

	private void triangle( Triangle t ) 
	{
		floatArrayBuilder.put( t.m0.currentPosition );
		floatArrayBuilder.put( t.m0.normal );
		
		floatArrayBuilder.put( t.m1.currentPosition );
		floatArrayBuilder.put( t.m1.normal );
		
		floatArrayBuilder.put( t.m2.currentPosition );
		floatArrayBuilder.put( t.m2.normal );
	}
	
	public void dispose() 
	{
		if ( polyProgram != null ) {
			polyProgram.dispose();
			polyProgram=null;
		}
		if ( vbo != null ) {
			vbo.dispose();
			vbo=null;
		}
	}
	
	protected static boolean noSideExceedsLengthSquared(Mass m0, Mass m1, Mass m2, double lengthSquared) 
	{
		return m0.currentPosition.dst2( m1.currentPosition ) <= lengthSquared && m0.currentPosition.dst2( m2.currentPosition ) <= lengthSquared;
	}
	
	protected final class Triangle {

		public Mass m0;
		public Mass m1;
		public Mass m2;

		public Triangle() {
		}
		public void set(Mass m0,Mass m1,Mass m2) {
			this.m0 = m0;
			this.m1 = m1;
			this.m2 = m2;
		}
		
		@Override
		public String toString() {
			return "Triangle[ "+m0.currentPosition+" -> "+m1.currentPosition+" -> "+m2.currentPosition+" ]"; 
		}

		public boolean noSideExceedsLengthSquared(double lengthSquared) 
		{
			return m0.currentPosition.dst2( m1.currentPosition ) <= lengthSquared && m0.currentPosition.dst2( m2.currentPosition ) <= lengthSquared;
		}
	}	
}