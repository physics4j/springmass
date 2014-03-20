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

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Label;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

import de.codesourcery.springmass.simulation.Mass;
import de.codesourcery.springmass.simulation.SimulationParameters;
import de.codesourcery.springmass.simulation.Simulator;
import de.codesourcery.springmass.simulation.Spring;
import de.codesourcery.springmass.simulation.SpringMassSystem;
import de.codesourcery.springmass.ui.AbstractCameraController;
import de.codesourcery.springmass.ui.FPSCameraController;
import de.codesourcery.springmass.ui.ICameraController;
import de.codesourcery.springmass.ui.IRenderPanel;

public class OpenGLRenderPanel implements IRenderPanel , Screen 
{
	/**
	 * Max. number of vertices that may be passed to glDrawArrays() at once. 
	 */
	public static final int GL_DRAW_ARRAYS_MAX_VERTEX_COUNT = 65535;
	
	private final DecimalFormat FPS_FORMAT = new DecimalFormat("###0");
	
	private final Object SIMULATION_LOCK = new Object();
	
	private volatile boolean frameRendered = false;
	
	private static final VertexAttributes FLATSHADER_VERTEX_ATTRIBUTES;	
	
	static 
	{
		final VertexAttribute positionAttr = VertexAttribute.Position();		
		positionAttr.alias = "a_position";
		
		final VertexAttribute normalAttr = VertexAttribute.Normal();
		normalAttr.alias = "a_normal";	
		
		final VertexAttribute colorAttr = VertexAttribute.ColorUnpacked();
		colorAttr.alias = "a_color";			

		FLATSHADER_VERTEX_ATTRIBUTES = new VertexAttributes( positionAttr , colorAttr ); // do NOT reorder the attributes here , use of FloatArrayBuilder#put() makes assumptions  !!!!		
	}
	
	// fake component used when constructing AWT events
	private static final Label DUMMY_COMPONENT = new Label("test");

	// @GuardedBy( SIMULATION_LOCK )
	private SpringMassSystem system;

	// @GuardedBy( SIMULATION_LOCK )
	private SimulationParameters parameters;
	
	private final FloatArrayBuilder floatArrayBuilder = new FloatArrayBuilder( 10240 , 10240 );
	
	private final DynamicVBO vbo;
	private final DynamicIBO ibo;
	
    private SpriteBatch spriteBatch;
    private BitmapFont font;
	
	private ClothRenderer clothRenderer;

	private int width;
	private int height;

	private ShaderProgram flatShaderProgram;
	
	private final List<MouseMotionListener> mouseMotionListener = new ArrayList<>();
	private final List<MouseListener> mouseListener = new ArrayList<>();
	private final List<KeyListener> keyListener = new ArrayList<>();
	
    private final MyCameraController cameraController;

	private final MyInputController inputProcessor;
	
	protected static enum Button {
		LEFT,RIGHT,MIDDLE;
	}
			
	protected class MyInputController extends FPSCameraController 
	{
		public MyInputController(Camera camera) 
		{
			super(camera, camera.direction );
		}

		@Override
		public boolean touchUp(int screenX, int screenY, int pointer, int button) 
		{
			boolean result = super.touchUp(screenX, screenY, pointer, button);
			if ( ! mouseListener.isEmpty() ) 
			{
				final MouseEvent event = toMouseEvent(screenX,screenY);				
				for ( MouseListener l : mouseListener ) {
					l.mouseReleased( event );
				}
				result = true;
			}
			return result;
		}
		
		private MouseEvent toMouseEvent(int screenX,int screenY) {
			return new MouseEvent(DUMMY_COMPONENT,123,System.currentTimeMillis(),0,screenX,screenY,1,false,MouseEvent.BUTTON2);			
		}
		
		private MouseEvent toMouseClickEvent(int screenX,int screenY,Button button) 
		{
			final int code;
			switch(button) {
			case LEFT:
				code = MouseEvent.BUTTON1;
				break;
			case MIDDLE:
				code = MouseEvent.BUTTON2;
				break;
			case RIGHT:
				code = MouseEvent.BUTTON3;
				break;
			default:
				throw new RuntimeException("Error");
			}
			return new MouseEvent(DUMMY_COMPONENT,123,System.currentTimeMillis(),0,screenX,screenY,1,false,code);
		}		
		
		@Override
		public boolean touchDragged(int screenX, int screenY, int pointer) 
		{
			boolean result = super.touchDragged(screenX, screenY, pointer);
			if ( ! mouseMotionListener.isEmpty() ) 
			{
				final MouseEvent event = toMouseEvent(screenX,screenY);				
				for (MouseMotionListener l : mouseMotionListener) {
					l.mouseDragged( event );
				}
				result = true;
			}			
			return result;
		}
		
		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button) 
		{
			boolean result = super.touchDown(screenX, screenY, pointer, button);
			if ( ! mouseListener.isEmpty() ) 
			{
				final MouseEvent event = toMouseEvent(screenX,screenY);				
				for ( MouseListener l : mouseListener ) {
					l.mousePressed( event );
				}
				result = true;
			}			
			return result;
		}
		
		@Override
		public boolean scrolled(int amount) {
			return false;
		}
		
		@Override
		public boolean mouseMoved(int screenX, int screenY) {
			boolean result = super.mouseMoved( screenX , screenY);
			if ( ! mouseMotionListener.isEmpty() ) 
			{
				final MouseEvent event = toMouseEvent(screenX,screenY);				
				for (MouseMotionListener l : mouseMotionListener) {
					l.mouseMoved( event );
				}
				result=true;
			}				
			return result;
		}
		
		@Override
		public boolean keyUp(int keycode) 
		{
			boolean result = super.keyUp(keycode);
			if ( ! keyListener.isEmpty() ) 
			{
				final KeyEvent event = toKeyEvent(keycode);				
				for ( KeyListener l : keyListener ) {
					l.keyReleased( event );
				}
			}			
			return result;
		}
		
		@Override
		public boolean keyTyped(char character) 
		{
			boolean result = super.keyTyped(character);
			
			if ( ! keyListener.isEmpty() ) 
			{
				final KeyEvent event = toKeyEvent(character);
				for ( KeyListener l : keyListener ) {
					l.keyTyped( event );
				}
				result=true;
			}
			return result;
		}
		
		private KeyEvent toKeyEvent(char character) {
			final int keyCode = KeyEvent.getExtendedKeyCodeForChar(character);
			return new KeyEvent(DUMMY_COMPONENT, 123 , System.currentTimeMillis() , 0, keyCode , character );
		}
		
		private KeyEvent toKeyEvent(int keyCode) {
			return new KeyEvent(DUMMY_COMPONENT, 123 , System.currentTimeMillis() , 0, keyCode , KeyEvent.CHAR_UNDEFINED );
		}		
		
		@Override
		public boolean keyDown(int keycode) {
			boolean result = super.keyDown(keycode);
			if ( ! keyListener.isEmpty() ) 
			{
				final KeyEvent event = toKeyEvent(keycode);
				for ( KeyListener l : keyListener ) {
					l.keyPressed( event );
				}
				result=true;
			}
			return result;			
		}

		@Override
		public boolean canTranslateCamera(Camera cam, Vector3 posDelta) {
			return true;
		}

		@Override
		public void cameraTranslated(Camera camera) {
		}

		@Override
		public void cameraRotated(Camera camera) {
		}

		@Override
		public void onLeftClick(int screenX,int screenY) 
		{
			if ( ! mouseListener.isEmpty() ) 
			{
				System.out.println("Left click: "+screenX+","+screenY);
				final MouseEvent event = toMouseClickEvent(screenX,screenY,Button.LEFT); 
				for ( MouseListener l : mouseListener ) {
					l.mousePressed( event );
				}
			}					
		}

		@Override
		public void onRightClick(int screenX,int screenY) 
		{
			if ( ! mouseListener.isEmpty() ) 
			{
				final MouseEvent event = toMouseClickEvent(screenX,screenY,Button.RIGHT);
				for ( MouseListener l : mouseListener ) {
					l.mousePressed( event );
				}
			}			
		}
	};

	protected class MyCameraController extends AbstractCameraController {
		
		public final PerspectiveCamera camera;
		
		public MyCameraController(int width,int height) 
		{
			this.camera = new PerspectiveCamera();
			this.camera.position.set( 0 , 0 , 2000 );
			this.camera.far = 100000;
			this.camera.near = 1;
			this.camera.fieldOfView = 60;
			this.camera.direction.set( 0, 0, -1 );
			this.camera.up.set( 0, 1, 0 );	
			viewportChanged( width , height);
		}
		
		@Override
		public void viewportChanged(int width, int height) 
		{
			super.viewportChanged(width, height);
			
			camera.viewportHeight = height;
			camera.viewportWidth = width;
			camera.update(true);			
		}
		
		@Override
		public void cameraChanged() {
			super.cameraChanged();
			camera.update(true);
		}

		@Override
		public Vector3 getPosition() { return camera.position; }

		@Override
		public void setPosition(Vector3 position) { camera.position.set( position); }

		@Override
		public Vector3 getViewDirection() { return camera.direction; }

		@Override
		public void setViewDirection(Vector3 dir) { camera.direction.set( dir ); camera.direction.nor(); }
	}
	
	public OpenGLRenderPanel(int width,int height) throws IOException
	{
		cameraController = new MyCameraController(width, height);
		
		this.inputProcessor=new MyInputController( cameraController.camera );

		clothRenderer = new ClothRenderer();
		
		this.vbo = new DynamicVBO(false , 65536 , 7*4 );
		this.ibo = new DynamicIBO(false , 65536 );
		
		try {
			flatShaderProgram = ShaderUtils.loadShader("lines");
		} 
		catch(Exception e) 
		{
			clothRenderer.dispose();
			throw new IOException("Failed to load 'lines' shader: "+e.getMessage(),e);
		} 		
		
        spriteBatch = new SpriteBatch();
        font = new BitmapFont();
        
		resize(width,height);        
        
		Gdx.input.setInputProcessor( inputProcessor );
	}
	
	/* (non-Javadoc)
	 * @see de.codesourcery.springmass.springmass.IRenderPanel#setSimulator(de.codesourcery.springmass.springmass.Simulator)
	 */
	@Override
	public void setSimulator(Simulator simulator) 
	{
		synchronized (SIMULATION_LOCK) 
		{
			this.system = simulator.getSpringMassSystem().createCopy();
			this.parameters = simulator.getSimulationParameters();
		}
	}

	@Override
	public void addTo(Container container) 
	{
	}

	/* (non-Javadoc)
	 * @see de.codesourcery.springmass.springmass.IRenderPanel#viewToModel(int, int)
	 */
	@Override
	public Vector3 viewToModel(int x,int y) 
	{
		final Vector3 tmp = new Vector3(x,y,0); // z = = => point on NEAR plane
		cameraController.camera.unproject( tmp );
		return tmp;
	}

	/* (non-Javadoc)
	 * @see de.codesourcery.springmass.springmass.IRenderPanel#modelToView(de.codesourcery.springmass.math.Vector4)
	 */
	@Override
	public Point modelToView(Vector3 vec) 
	{
		final Vector3 tmp = new Vector3( vec );
		cameraController.camera.project( tmp );
		return new Point( (int) Math.round(tmp.x) , Math.round(tmp.y ) );
	}

	/* (non-Javadoc)
	 * @see de.codesourcery.springmass.springmass.IRenderPanel#modelChanged()
	 */
	@Override
	public void modelChanged() 
	{
		final boolean wait;
		synchronized (SIMULATION_LOCK) 
		{
			this.system.updateFromOriginal();
			wait = this.parameters.isWaitForVSync();
			frameRendered = false;				
		}
		while ( wait && ! frameRendered ) {
			// spin-waiting
		}
	}

	@Override
	public boolean renderFrame(float currentFPS) 
	{
		synchronized (SIMULATION_LOCK) 
		{
			final SimulationParameters params  = this.parameters;
			final SpringMassSystem sys  = this.system;
			render( sys , params  , currentFPS );
			frameRendered = true;			
		}
		// TODO: Maybe needed?
		// Toolkit.getDefaultToolkit().sync();
		return true;
	}
	
	private void render(SpringMassSystem system,SimulationParameters parameters,float currentAvgFPS) 
	{
		inputProcessor.update();
		
		// clear buffers
		final GL20 gl20 = Gdx.graphics.getGL20();
		gl20.glClearColor( 0 , 0, 0, 1 );
		gl20.glClear( GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT );

		// setup transformation matrices
		final Matrix4 modelMatrix = new Matrix4().idt();
		modelMatrix.scale(2f,2f,1f);
		
		final Matrix4 modelViewMatrix = new Matrix4( cameraController.camera.view );
		modelViewMatrix.mul(  modelMatrix );
		
		final Matrix4 modelViewProjectionMatrix = new Matrix4( cameraController.camera.projection );
		modelViewProjectionMatrix.mul(  modelViewMatrix );	
		
		// render cloth
		clothRenderer.renderCloth(modelMatrix, modelViewMatrix, modelViewProjectionMatrix, system , parameters );

		if ( parameters.isRenderMasses() ) 
		{
			renderMasses(modelViewProjectionMatrix);
		}

		// render springs
        if ( parameters.isRenderSprings() || parameters.isRenderAllSprings() ) 
        {
        	renderSprings(modelViewProjectionMatrix);
        }
        
        // render FPS
		gl20.glDisable(GL20.GL_DEPTH_TEST);
		
		spriteBatch.getProjectionMatrix().setToOrtho2D( 0 , 0 , width , height );
		spriteBatch.setColor( 1 , 0 , 0, 1 );
		
        spriteBatch.begin();
        
        font.draw(spriteBatch, "FPS: "+FPS_FORMAT.format( currentAvgFPS )+" ( "+this.cameraController.camera.position+") ", 10, height - 20 );
        spriteBatch.end();
	}
	
	private void renderSprings(Matrix4 modelViewProjectionMatrix) 
	{
		final GL20 gl20 = Gdx.gl20;
		
		gl20.glDisable(GL20.GL_DEPTH_TEST);
		gl20.glEnable(GL11.GL_POLYGON_OFFSET_LINE);
		gl20.glPolygonOffset(1.0f, 1.0f);            
        
		flatShaderProgram.begin();
		flatShaderProgram.setUniformMatrix("u_modelViewProjection" , modelViewProjectionMatrix );
		
		floatArrayBuilder.begin();
		
		// bind VBO
		vbo.bind( flatShaderProgram , FLATSHADER_VERTEX_ATTRIBUTES );
		
		int vertexCount = 0;
        for ( Spring s : system.getSprings() ) 
        {
            if ( s.doRender ) 
            {
				final float r = s.color.getRed()/255.0f;
				final float g = s.color.getGreen()/255.0f;
				final float b = s.color.getBlue()/255.0f;
				
				floatArrayBuilder.put( s.m1.currentPosition , 1 ); // 4 floats
				floatArrayBuilder.put( r , g , b ); // 3 floats
				
				floatArrayBuilder.put( s.m2.currentPosition  , 1 );
				floatArrayBuilder.put( r , g , b );
				
				vertexCount += 2;
				if ( (vertexCount+2) >= GL_DRAW_ARRAYS_MAX_VERTEX_COUNT ) 
				{
					// populate buffers
					final int size = floatArrayBuilder.end();
					vbo.setVertices( floatArrayBuilder.array , 0 , size );
					
					// gl20.glDrawElements(GL20.GL_LINES , index , GL20.GL_UNSIGNED_SHORT , 0 );        			
					gl20.glDrawArrays(GL20.GL_LINES, 0 , vertexCount );
					
					vertexCount  = 0;
					floatArrayBuilder.begin();
				}
            }
        }

        if ( vertexCount > 0) 
        {
			// populate buffers
			final int size = floatArrayBuilder.end();
			vbo.setVertices( floatArrayBuilder.array , 0 , size );
			
			// render
			// gl20.glDrawElements(GL20.GL_LINES , index , GL20.GL_UNSIGNED_SHORT , 0 );        			
			gl20.glDrawArrays(GL20.GL_LINES, 0 , vertexCount );
						
        }
        flatShaderProgram.end();
        
		// unbind VBO
		vbo.unbind( flatShaderProgram , FLATSHADER_VERTEX_ATTRIBUTES);
		
		gl20.glDisable(GL11.GL_POLYGON_OFFSET_LINE);            
	}
	
	private void renderMasses(Matrix4 modelViewProjectionMatrix) 
	{
		final GL20 gl20 = Gdx.gl20;
		
		gl20.glDisable(GL20.GL_DEPTH_TEST);
		
		gl20.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
		gl20.glPolygonOffset(1.0f, 1.0f);		
		
		flatShaderProgram.begin();
		flatShaderProgram.setUniformMatrix("u_modelViewProjection" , modelViewProjectionMatrix );
		
		floatArrayBuilder.begin();
		
		// bind VBO
		vbo.bind( flatShaderProgram , FLATSHADER_VERTEX_ATTRIBUTES );
		
		final float dx = 1;
		final float dy = 1;
		
		int indices = 0;
		
		final Mass[][] array = system.getMassArray();
		for ( int y = 0 ; y < parameters.getGridRowCount() ; y++ ) 
		{
			for ( int x = 0 ; x < parameters.getGridColumnCount() ; x++ ) 
			{
				final Mass m = array[x][y];
				
				final float r;
				final float g;
				final float b;							
				if ( m.isSelected() ) 
				{
					r = 1; // red
					g = 0;
					b = 0;
				} 
				else 
				{
					if ( m.isFixed() ) 
					{
						r=0;
						g=0;
						b=1; // blue 
					} 
					else 
					{
						r = m.color.getRed()/255.0f;
						g = m.color.getGreen()/255.0f;
						b = m.color.getBlue()/255.0f;
					}
				}
				
				filledRect( m.currentPosition , dx , dy , r , g , b );
				indices += 6;
				
				if ( (indices+6) >= OpenGLRenderPanel.GL_DRAW_ARRAYS_MAX_VERTEX_COUNT ) 
				{
					// populate buffers
					final int size = floatArrayBuilder.end();
					vbo.setVertices( floatArrayBuilder.array , 0 , size );
					
					// gl20.glDrawElements(GL20.GL_LINES , index , GL20.GL_UNSIGNED_SHORT , 0 );        			
					gl20.glDrawArrays(GL20.GL_TRIANGLES, 0 , indices );
					
					indices  = 0;
					floatArrayBuilder.begin();					
				}
			}
		}
		
		if ( indices > 0 ) 
		{
			// populate buffers
			final int size = floatArrayBuilder.end();
			vbo.setVertices( floatArrayBuilder.array , 0 , size );
			
			// gl20.glDrawElements(GL20.GL_LINES , index , GL20.GL_UNSIGNED_SHORT , 0 );        			
			gl20.glDrawArrays(GL20.GL_TRIANGLES, 0 , indices );
		}
		
		vbo.unbind( flatShaderProgram ,  FLATSHADER_VERTEX_ATTRIBUTES );
		flatShaderProgram.end();

		gl20.glEnable(GL11.GL_DEPTH_TEST);			
		gl20.glDisable(GL11.GL_POLYGON_OFFSET_FILL);			
	}
	
	private void filledRect( Vector3 position , float dx , float dy , float r, float g , float b) {
		
		float x0 = position.x-dx ; float y0 = position.y+dy ; float z = position.z;
		float x1 = position.x-dx ; float y1 = position.y-dy ; 
		float x2 = position.x+dx ; float y2 = position.y-dy ;
		float x3 = position.x+dx ; float y3 = position.y+dy ;
		
		triangle( x0 , y0 , z , x1 , y1 , z , x3 , y3 , z , r, g , b ); // p0 -> p1 -> p3
		triangle( x1 , y1 , z , x2 , y2 , z , x3 , y3 , z , r, g , b ); // p1 -> p2 -> p3
	}
	
	private void triangle(float x0,float y0,float z0, float x1,float y1,float z1, float x2,float y2,float z2, float r, float g , float b) 
	{
		vertex( x0 , y0 , z0 , r , g , b );
		vertex( x1 , y1 , z1 , r , g , b );
		vertex( x2 , y2 , z2 , r , g , b );
	}
	
	private void line (float x0,float y0,float z0, float x1,float y1,float z1, float r, float g , float b) 
	{
		vertex( x0 , y0 , z0 , r, g , b );
		vertex( x1 , y1 , z1 , r, g , b );
	}	
	
	private void vertex(float x,float y,float z, float r, float g , float b) {
		floatArrayBuilder.put( x , y , z , 1 ); // 4 floats
		floatArrayBuilder.put( r , g , b ); // 3 floats		
	}

	// ==== libgdx ====

	@Override
	public void dispose() 
	{
		if ( spriteBatch != null ) {
			spriteBatch.dispose();
			spriteBatch = null;
		}
		if ( font != null ) {
			font.dispose();
			font=null;
		}
		if ( clothRenderer != null ) {
			clothRenderer.dispose();
			clothRenderer = null;
		}
		
		if ( flatShaderProgram != null ) {
			flatShaderProgram.dispose();
			flatShaderProgram=null;
		}		
		if ( vbo != null ) {
			vbo.dispose();
		}
		if ( ibo != null ) {
			ibo.dispose();
		}
	}

	@Override
	public void hide() {
		// TODO Auto-generated method stub
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
	}

	@Override
	public void render(float deltaT) {
		renderFrame( 1f/deltaT );
	}

	@Override
	public void resize(int width, int height) 
	{
		System.out.println("Resize: "+width+" x "+height);
		this.width = width;
		this.height = height;
		
		spriteBatch.getProjectionMatrix().setToOrtho2D( 0 ,  0 ,  width ,  height );
		cameraController.viewportChanged(width, height);
	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub

	}

	@Override
	public void show() {
		// TODO Auto-generated method stub

	}

	@Override
	public void addKeyListener(KeyListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be NULL");
		}
		this.keyListener.add( listener );
	}

	@Override
	public void addMouseListener(MouseListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be NULL");
		}
		this.mouseListener.add( listener );
	}

	@Override
	public void addMouseMotionListener(MouseMotionListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("listener must not be NULL");
		}
		this.mouseMotionListener.add( listener );
	}

	@Override
	public void setPreferredSize(Dimension preferredSize) {
		resize(preferredSize.width ,preferredSize.height);
	}

	@Override
	public ICameraController getCameraController() {
		return cameraController;
	}
}