package de.codesourcery.springmass.ui.opengl;

/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.BufferUtils;

/**
 * VBO whose backing buffer is dynamically expanded as needed.
 * 
 * <p>Note that this class will never shrink the buffer once it has been expanded.</p>
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class DynamicVBO 
{
	private static final IntBuffer tmpHandle = BufferUtils.newIntBuffer(1);

	private FloatBuffer buffer;
	private ByteBuffer byteBuffer;
	
	private int bufferHandle;
	private final int usage;
	private boolean isDirty = false;
	private boolean isBound = false;

	/** Constructs a new interleaved VertexBufferObject.
	 * 
	 * @param isStatic whether the vertex data is static.
	 * @param numVertices the maximum number of vertices
	 * @param attributes the {@link VertexAttributes}. */
	public DynamicVBO (boolean isStatic, int numVertices, int vertexSizeInBytes) 
	{
		allocBuffer( numVertices * vertexSizeInBytes );
		usage = isStatic ? GL11.GL_STATIC_DRAW : GL11.GL_DYNAMIC_DRAW;
	}
	
	private void allocBuffer(int sizeInBytes) 
	{
		byteBuffer = BufferUtils.newUnsafeByteBuffer( sizeInBytes );
		buffer = byteBuffer.asFloatBuffer();
		buffer.flip();
		byteBuffer.flip();
		bufferHandle = createBufferObject();		
	}
	
	private void disposeBuffer() 
	{
		GL20 gl = Gdx.gl20;
		
		if ( byteBuffer != null ) 
		{
			tmpHandle.clear();
			tmpHandle.put(bufferHandle);
			tmpHandle.flip();
	
			gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
			gl.glDeleteBuffers(1, tmpHandle);
			bufferHandle = 0;
			BufferUtils.disposeUnsafeByteBuffer(byteBuffer);
			byteBuffer = null;
			buffer = null; 
		}
	}

	private static int createBufferObject () 
	{
		Gdx.gl20.glGenBuffers(1, tmpHandle);
		return tmpHandle.get(0);
	}

	public int getNumVertices (int vertexSizeInBytes) {
		return buffer.limit() * 4 / vertexSizeInBytes;
	}

	/**
	 * Returns the max. number of vertices size buffer can currently hold.
	 * @return
	 */
	public int getNumMaxVertices (int vertexSizeInBytes) {
		return byteBuffer.capacity() / vertexSizeInBytes;
	}

	public FloatBuffer getBuffer () {
		isDirty = true;
		return buffer;
	}

	public void setVertices (float[] vertices, int offset, int count) 
	{
		isDirty = true;
		
		if ( count > buffer.capacity() ) 
		{
			System.out.println("Resizing VBO : "+buffer.capacity()+" -> "+count);
			disposeBuffer();
			allocBuffer( count*4 );
		}
		BufferUtils.copy(vertices, byteBuffer, count, offset);
		buffer.position(0);
		buffer.limit(count);

		if (isBound) {
			GL20 gl = Gdx.gl20;
			gl.glBufferData(GL20.GL_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage);
			isDirty = false;
		}
	}

	public void bind (ShaderProgram shader, VertexAttributes attributes) 
	{
		final GL20 gl = Gdx.gl20;

		gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, bufferHandle);
		if (isDirty) {
			byteBuffer.limit(buffer.limit() * 4);
			gl.glBufferData(GL20.GL_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage);
			isDirty = false;
		}

		final int numAttributes = attributes.size();
		for (int i = 0; i < numAttributes; i++) {
			final VertexAttribute attribute = attributes.get(i);
			final int location = shader.getAttributeLocation(attribute.alias);
			if (location < 0) {
				continue;
			}
			shader.enableVertexAttribute(location);

			if (attribute.usage == Usage.ColorPacked) {
				shader.setVertexAttribute(location, attribute.numComponents, GL20.GL_UNSIGNED_BYTE, true, attributes.vertexSize, attribute.offset);
			} else {
				shader.setVertexAttribute(location, attribute.numComponents, GL20.GL_FLOAT, false, attributes.vertexSize, attribute.offset);
			}
		}
		isBound = true;
	}

	public void unbind (final ShaderProgram shader, VertexAttributes attributes) 
	{
		final GL20 gl = Gdx.gl20;
		final int numAttributes = attributes.size();
		for (int i = 0; i < numAttributes; i++) {
			shader.disableVertexAttribute(attributes.get(i).alias);
		}
		gl.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
		isBound = false;
	}

	public void invalidate () {
		bufferHandle = createBufferObject();
		isDirty = true;
	}

	public void dispose () 
	{
		disposeBuffer();
	}
}