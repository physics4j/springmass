package de.codesourcery.springmass.ui.opengl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL11;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;

/** <p>
 *
 * @author tobias.gierke@code-sourcery.de
 */
public class DynamicIBO 
{
	final static IntBuffer tmpHandle = BufferUtils.newIntBuffer(1);

	private ShortBuffer buffer;
	private ByteBuffer byteBuffer;
	private int bufferHandle;
	private boolean isDirty = true;
	private boolean isBound = false;
	private final int usage;

	/** Creates a new IndexBufferObject.
	 * 
	 * @param isStatic whether the index buffer is static
	 * @param maxIndices the maximum number of indices this buffer can hold */
	public DynamicIBO(boolean isStatic, int maxIndices) 
	{
		allocBuffer( maxIndices );
		usage = isStatic ? GL11.GL_STATIC_DRAW : GL11.GL_DYNAMIC_DRAW;
	}

	private void allocBuffer(int indices) {
		byteBuffer = BufferUtils.newUnsafeByteBuffer(indices * 2);
		buffer = byteBuffer.asShortBuffer();
		buffer.flip();
		byteBuffer.flip();
		bufferHandle = createBufferObject();		
	}
	
	private void disposeBuffer() 
	{
		if ( byteBuffer != null ) 
		{
			tmpHandle.clear();
			tmpHandle.put(bufferHandle);
			tmpHandle.flip();
			GL20 gl = Gdx.gl20;
			gl.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);
			gl.glDeleteBuffers(1, tmpHandle);
			bufferHandle = 0;
			BufferUtils.disposeUnsafeByteBuffer(byteBuffer);
			byteBuffer = null;
			buffer = null;
		}
	}
	
	private static int createBufferObject () {
		Gdx.gl20.glGenBuffers(1, tmpHandle);
		return tmpHandle.get(0);
	}

	public int getNumIndices () {
		return buffer.limit();
	}

	public int getNumMaxIndices () {
		return buffer.capacity();
	}

	public void setIndices (short[] indices, int offset, int count) 
	{
		isDirty = true;
		
		if ( buffer.capacity() < count ) {
			System.out.println("Resizing IBO : "+buffer.capacity()+" -> "+count);			
			disposeBuffer();
			allocBuffer(count);
		} else {
			buffer.clear();
		}
		buffer.put(indices, offset, count);
		buffer.flip();
		byteBuffer.position(0);
		byteBuffer.limit(count << 1);

		if (isBound) {
			GL20 gl = Gdx.gl20;
			gl.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage);
			isDirty = false;
		}
	}

	/** <p>
	 * Returns the underlying ShortBuffer. If you modify the buffer contents they wil be uploaded on the call to {@link #bind()}.
	 * If you need immediate uploading use {@link #setIndices(short[], int, int)}.
	 * </p>
	 * 
	 * @return the underlying short buffer. */
	public ShortBuffer getBuffer () {
		isDirty = true;
		return buffer;
	}

	/** Binds this IndexBufferObject for rendering with glDrawElements. */
	public void bind () 
	{
		if (bufferHandle == 0) {
			throw new GdxRuntimeException("No buffer allocated!");
		}

		GL20 gl = Gdx.gl20;
		gl.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, bufferHandle);
		if (isDirty) {
			byteBuffer.limit(buffer.limit() * 2);
			gl.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage);
			isDirty = false;
		}
		isBound = true;
	}

	/** Unbinds this IndexBufferObject. */
	public void unbind () {
		Gdx.gl20.glBindBuffer(GL11.GL_ELEMENT_ARRAY_BUFFER, 0);
		isBound = false;
	}

	/** Invalidates the IndexBufferObject so a new OpenGL buffer handle is created. Use this in case of a context loss. */
	public void invalidate () {
		bufferHandle = createBufferObject();
		isDirty = true;
	}

	/** Disposes this IndexBufferObject and all its associated OpenGL resources. */
	public void dispose () {
		disposeBuffer();
	}
}