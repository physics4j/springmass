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

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

import de.codesourcery.springmass.simulation.*;
import de.codesourcery.springmass.ui.*;

public class OpenGLRenderPanel implements IRenderPanel , Screen 
{
	/**
	 * Max. number of vertices that may be passed to glDrawArrays() at once. 
	 */
	public static final int GL_DRAW_ARRAYS_MAX_VERTEX_COUNT = 65535;
	
	private final DecimalFormat FPS_FORMAT = new DecimalFormat("###0");
	
	private final Object SIMULATION_LOCK = new Object();
	
	private volatile boolean frameRendered = false;
	
	private static final VertexAttributes LINE_VERTEX_ATTRIBUTES;	
	
	static 
	{
		final VertexAttribute positionAttr = VertexAttribute.Position();		
		positionAttr.alias = "a_position";
		
		final VertexAttribute normalAttr = VertexAttribute.Normal();
		normalAttr.alias = "a_normal";	
		
		final VertexAttribute colorAttr = VertexAttribute.ColorUnpacked();
		colorAttr.alias = "a_color";			

		LINE_VERTEX_ATTRIBUTES = new VertexAttributes( positionAttr , colorAttr ); // do NOT reorder the attributes here , use of FloatArrayBuilder#put() makes assumptions  !!!!		
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

	private ShaderProgram wireProgram;
	
	private final List<MouseMotionListener> mouseMotionListener = new ArrayList<>();
	private final List<MouseListener> mouseListener = new ArrayList<>();
	private final List<KeyListener> keyListener = new ArrayList<>();
	
    private final MyCameraController cameraController;

	private final MyInputController inputProcessor;
			
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
				for ( MouseListener l : mouseListener ) {
					l.mouseReleased( toMouseEvent(screenX,screenY) );
				}
				result = true;
			}
			return result;
		}
		
		private MouseEvent toMouseEvent(int screenX,int screenY) {
			return new MouseEvent(DUMMY_COMPONENT,123,System.currentTimeMillis(),0,screenX,screenY,1,false,MouseEvent.BUTTON2);			
		}
		
		@Override
		public boolean touchDragged(int screenX, int screenY, int pointer) 
		{
			boolean result = super.touchDragged(screenX, screenY, pointer);
			if ( ! mouseMotionListener.isEmpty() ) 
			{
				for (MouseMotionListener l : mouseMotionListener) {
					l.mouseDragged( toMouseEvent(screenX,screenY) );
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
				for ( MouseListener l : mouseListener ) {
					l.mousePressed( toMouseEvent(screenX,screenY) );
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
				for (MouseMotionListener l : mouseMotionListener) {
					l.mouseMoved( toMouseEvent(screenX,screenY) );
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
				for ( KeyListener l : keyListener ) {
					l.keyReleased( toKeyEvent(keycode) );
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
				for ( KeyListener l : keyListener ) {
					l.keyTyped( toKeyEvent(character) );
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
			if ( keyListener != null ) 
			{
				for ( KeyListener l : keyListener ) {
					l.keyPressed( toKeyEvent(keycode) );
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
		public void onLeftClick() {
		}

		@Override
		public void onRightClick() {
		}
	};

	protected class MyCameraController extends AbstractCameraController {
		
		public final PerspectiveCamera camera;
		
		public MyCameraController(int width,int height) 
		{
			this.camera = new PerspectiveCamera();
			this.camera.position.set( 500 , -300 , 1000 );
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
			wireProgram = ShaderUtils.loadShader("lines");
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

	private int getWidth() {
		return width;
	}

	private int getHeight() {
		return height;
	}

	@Override
	public void addTo(Container container) 
	{
	}

	/* (non-Javadoc)
	 * @see de.codesourcery.springmass.springmass.IRenderPanel#viewToModel(int, int)
	 */
	@Override
	public Vector3 viewToModel(int x,int y) {

		float scaleX = getWidth() / (float) parameters.getXResolution();
		float scaleY = getHeight() / (float) parameters.getYResolution();
		return new Vector3( x / scaleX , y / scaleY , 0 );
	}

	/* (non-Javadoc)
	 * @see de.codesourcery.springmass.springmass.IRenderPanel#modelToView(de.codesourcery.springmass.math.Vector4)
	 */
	@Override
	public Point modelToView(Vector3 vec) 
	{
		double scaleX = getWidth() / (double) parameters.getXResolution();
		double scaleY = getHeight() / (double) parameters.getYResolution();
		return modelToView( vec , scaleX , scaleY ); 
	}

	/* (non-Javadoc)
	 * @see de.codesourcery.springmass.springmass.IRenderPanel#modelToView(de.codesourcery.springmass.math.Vector4, double, double)
	 */
	@Override
	public Point modelToView(Vector3 vec,double scaleX,double scaleY) 
	{
		return new Point( (int) Math.round( vec.x * scaleX ) , (int) Math.round( vec.y * scaleY ) );
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
        
        font.draw(spriteBatch, "FPS: "+FPS_FORMAT.format( currentAvgFPS ), 10, height - 20 );
        spriteBatch.end();
	}
	
	private void renderSprings(Matrix4 modelViewProjectionMatrix) 
	{
		final GL20 gl20 = Gdx.gl20;
		
		gl20.glDisable(GL20.GL_DEPTH_TEST);
		gl20.glEnable(GL11.GL_POLYGON_OFFSET_LINE);
		gl20.glPolygonOffset(1.0f, 1.0f);            
        
		wireProgram.begin();
		wireProgram.setUniformMatrix("u_modelViewProjection" , modelViewProjectionMatrix );
		
		floatArrayBuilder.begin();
		
		// bind VBO
		vbo.bind( wireProgram , LINE_VERTEX_ATTRIBUTES );
		
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
        wireProgram.end();
        
		// unbind VBO
		vbo.unbind( wireProgram , LINE_VERTEX_ATTRIBUTES);
		
		gl20.glDisable(GL11.GL_POLYGON_OFFSET_LINE);            
	}
	
	private void renderMasses(Matrix4 modelViewProjectionMatrix) 
	{
		final GL20 gl20 = Gdx.gl20;
		
		final VertexInfo v0 = new VertexInfo();
		final VertexInfo v1 = new VertexInfo();
		final VertexInfo v2 = new VertexInfo();
		final VertexInfo v3 = new VertexInfo();		
		
		final MeshBuilder builder = new MeshBuilder();
		builder.begin( LINE_VERTEX_ATTRIBUTES , GL20.GL_TRIANGLES );
		
		final float dx = 3;
		final float dy = 3;
		
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
					
				v0.setCol( r ,  g,  b,  1 );
				v1.setCol( r ,  g,  b,  1 );
				v2.setCol( r ,  g,  b,  1 );
				v3.setCol( r ,  g,  b,  1 );							
					
				v0.setPos( m.currentPosition.x-dx , m.currentPosition.y+dy , m.currentPosition.z );
				v1.setPos( m.currentPosition.x-dx , m.currentPosition.y-dy , m.currentPosition.z );
				v2.setPos( m.currentPosition.x+dx , m.currentPosition.y-dy , m.currentPosition.z );
				v3.setPos( m.currentPosition.x+dx , m.currentPosition.y+dy , m.currentPosition.z );

				builder.rect( v0 , v1, v2, v3);
			}
		}
		
		gl20.glDisable(GL20.GL_DEPTH_TEST);
		gl20.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
		gl20.glPolygonOffset(1.0f, 1.0f);
		   
		final Mesh mesh2 = builder.end();
		wireProgram.begin();
		wireProgram.setUniformMatrix("u_modelViewProjection" , modelViewProjectionMatrix );
		
		mesh2.render( wireProgram , GL20.GL_TRIANGLES );
		wireProgram.end();
		mesh2.dispose();
		
		gl20.glDisable(GL11.GL_POLYGON_OFFSET_FILL);			
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
		
		if ( wireProgram != null ) {
			wireProgram.dispose();
			wireProgram=null;
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