package de.codesourcery.springmass.render;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

import de.codesourcery.springmass.springmass.*;

public class OpenGLRenderPanel implements IRenderPanel , Screen {

	private final Object SIMULATION_LOCK = new Object();
	
	private static final VertexAttributes POLYGON_VERTEX_ATTRIBUTES;
	
	private static final boolean AVERAGE_NORMALS = true;
	
	private static final VertexAttributes LINE_VERTEX_ATTRIBUTES;	
	
	static 
	{
		final VertexAttribute positionAttr = VertexAttribute.Position();		
		positionAttr.alias = "a_position";
		
		final VertexAttribute normalAttr = VertexAttribute.Normal();
		normalAttr.alias = "a_normal";	
		
		final VertexAttribute colorAttr = VertexAttribute.ColorUnpacked();
		colorAttr.alias = "a_color";			

		POLYGON_VERTEX_ATTRIBUTES = new VertexAttributes( positionAttr , normalAttr );
		LINE_VERTEX_ATTRIBUTES = new VertexAttributes( positionAttr , colorAttr ); // do NOT reorder the attributes here , use of FloatArrayBuilder#put() makes assumptions  !!!!		
	}
	
	// fake component used when constructing AWT events
	private static final Label DUMMY_COMPONENT = new Label("test");

	// @GuardedBy( SIMULATION_LOCK )
	private SpringMassSystem system;

	// @GuardedBy( SIMULATION_LOCK )
	private SimulationParameters parameters;
	
	private final FloatArrayBuilder floatArrayBuilder = new FloatArrayBuilder( 10240 , 10240 );
	private final ShortArrayBuilder shortArrayBuilder = new ShortArrayBuilder(10240,10240);
	
	private final DynamicVBO vbo;
	private final DynamicIBO ibo;

	private int width;
	private int height;

	private final Object BUFFER_LOCK = new Object();

	private ShaderProgram polyProgram;
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
			return new KeyEvent(DUMMY_COMPONENT, 123 , System.currentTimeMillis() , 0, keyCode );
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
		
		this.vbo = new DynamicVBO(false , 65536 , 7*4 );
		this.ibo = new DynamicIBO(false , 65536 );
		
		resize(width,height);
		
		try {
			polyProgram = loadShader("default");
		} catch(Exception e) {
			throw new IOException("Failed to load 'default' shader: "+e.getMessage(),e);
		}
		
		try {
			wireProgram = loadShader("lines");
		} 
		catch(Exception e) 
		{
			polyProgram.dispose();
			throw new IOException("Failed to load 'lines' shader: "+e.getMessage(),e);
		} 		
		
		Gdx.input.setInputProcessor( inputProcessor );
	}
	
	private static ShaderProgram loadShader(String name) throws IOException {
		
		final String vShaderClasspathPath= "/vertexshader/"+name+"_vertex.glsl";
		final String fShaderClasspathPath= "/fragmentshader/"+name+"_fragment.glsl";
		
		final String vshader = loadFromClasspath( vShaderClasspathPath );
		final String fshader = loadFromClasspath( fShaderClasspathPath );
		
		System.out.println("==== vertex shader ====\n\n"+vshader);
		System.out.println("\n\n==== fragment shader ====\n\n"+fshader);
		final ShaderProgram result =  new ShaderProgram( vshader , fshader );
		if ( ! result.isCompiled() ) 
		{
			System.err.println("Shader compilation failed: "+result.getLog());
			throw new RuntimeException("Shader compilation failed");
		}
		return result;
	}
	
	private static String loadFromClasspath(String path) throws IOException {
		
		final InputStream in = OpenGLRenderPanel.class.getResourceAsStream( path );
		if ( in == null ) {
			throw new RuntimeException("Failed to load shader from classpath "+path);
		}
		final BufferedReader reader = new BufferedReader( new InputStreamReader(in) );
		try {
		StringBuilder builder = new StringBuilder();
		String line;
		
		while ( ( line = reader.readLine()) != null ) {
			builder.append( line+"\n" );
		}
		return builder.toString();
		} finally {
			reader.close();
		}
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

	protected final class Triangle {

		private Vector3 p0;
		private Vector3 p1;
		private Vector3 p2;
		
		private Vector3 normal0;
		private Vector3 normal1;
		private Vector3 normal2;		

		public Triangle() {
		}
		public void set(Vector3 p0,Vector3 p1,Vector3 p2,Vector3 normal0,Vector3 normal1,Vector3 normal2) {
			this.p0 = p0;
			this.p1 = p1;
			this.p2 = p2;
			this.normal0=normal0;
			this.normal1=normal1;
			this.normal2=normal2;
		}
		
		@Override
		public String toString() {
			return "Triangle[ "+p0+" -> "+p1+" -> "+p2+" ]"; 
		}

		public void populate(VertexInfo v0,VertexInfo v1,VertexInfo v2) {
			
			v0.setPos( p0.x , p0.y , p0.z );
			v1.setPos( p1.x , p1.y , p1.z );
			v2.setPos( p2.x , p2.y , p2.z );

			v0.setNor( normal0 );
			v1.setNor( normal1 );
			v2.setNor( normal2 );
		}

		public boolean noSideExceedsLengthSquared(double lengthSquared) 
		{
			return p0.dst2( p1 ) <= lengthSquared && p0.dst2( p2 ) <= lengthSquared;
		}
	}
	

	/* (non-Javadoc)
	 * @see de.codesourcery.springmass.springmass.IRenderPanel#modelChanged()
	 */
	@Override
	public void modelChanged() 
	{
		synchronized (SIMULATION_LOCK) 
		{                
			this.system.updateFromOriginal();
			if ( this.parameters.isWaitForVSync() ) 
			{
				try {
					SIMULATION_LOCK.wait();
				} 
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}        
	}

	public boolean renderFrame(float currentFPS) 
	{
		synchronized( BUFFER_LOCK ) 
		{
			final SimulationParameters params;
			final SpringMassSystem sys;
			synchronized (SIMULATION_LOCK) 
			{
				params = this.parameters;
				sys = this.system;
				render( sys , params  , currentFPS );
				SIMULATION_LOCK.notifyAll();
			}
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
		gl20.glClearColor( 1 , 1, 1, 1 );
		gl20.glClear( GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT );

		// setup transformations
		final Matrix4 modelMatrix = new Matrix4().idt();
		 modelMatrix.scale(2f,2f,1f);
		
		final Matrix4 modelViewMatrix = new Matrix4( cameraController.camera.view );
		modelViewMatrix.mul(  modelMatrix );
		
		final Matrix4 modelViewProjectionMatrix = new Matrix4( cameraController.camera.projection );
		modelViewProjectionMatrix.mul(  modelViewMatrix );	
		
		// render cloth
		renderCloth(modelMatrix,modelViewMatrix,modelViewProjectionMatrix);

		if ( parameters.isRenderMasses() ) 
		{
			renderMasses(modelViewProjectionMatrix);
		}

		// render springs
        if ( parameters.isRenderSprings() || parameters.isRenderAllSprings() ) 
        {
        	renderSprings(modelViewProjectionMatrix);
        }
	}
	
	private void renderCloth(Matrix4 modelMatrix,Matrix4 modelViewMatrix,Matrix4 modelViewProjectionMatrix) 
	{
		final GL20 gl20 = Gdx.graphics.getGL20();
		
		gl20.glEnable( GL20.GL_DEPTH_TEST );
		gl20.glDisable( GL20.GL_CULL_FACE );
		
		final int rows = parameters.getGridRowCount();
		final int columns = parameters.getGridColumnCount();

		final boolean checkArea = parameters.getMaxSpringLength() > 0;
		final double maxLenSquared = parameters.getMaxSpringLength()*parameters.getMaxSpringLength();

		polyProgram.begin();
		
		polyProgram.setUniformMatrix("u_modelViewProjection" , modelViewProjectionMatrix );
//		shaderProgram.setUniformf( "diffuseColor" , new Vector3(1,0,0) );
		
		polyProgram.setUniformMatrix( "u_modelView" , modelViewMatrix );
//		shaderProgram.setUniformMatrix( "normalMatrix", camera. );		
		polyProgram.setUniformf( "vLightPos" , new Vector3(100,-300,-1000) );
		
		MeshBuilder builder = new MeshBuilder();
		
		builder.begin(  POLYGON_VERTEX_ATTRIBUTES , GL20.GL_TRIANGLES );

		final VertexInfo v0 = new VertexInfo();
		final VertexInfo v1 = new VertexInfo();
		final VertexInfo v2 = new VertexInfo();
		
		final Triangle t1 = new Triangle();
		final Triangle t2 = new Triangle();	    
		
		final Mass[][] masses = system.getMassArray();
		for ( int y = 0 ; y < rows-1 ; y++) 
		{
			for ( int x = 0 ; x < columns-1 ; x++) 
			{
				Mass m0 = masses[x][y];
				Mass m1 = masses[x+1][y];
				Mass m2 = masses[x][y+1];
				Mass m3 = masses[x+1][y+1];

				Vector3 p0 = m0.currentPosition;
				Vector3 p1 = m1.currentPosition;
				Vector3 p2 = m2.currentPosition;
				Vector3 p3 = m3.currentPosition;

				t1.set(p0,p2,p1,m0.normal,m2.normal,m1.normal);
				t2.set(p1,p2,p3,m1.normal,m2.normal,m3.normal);	
				
				if ( checkArea ) 
				{
					if ( t1.noSideExceedsLengthSquared( maxLenSquared ) ) 
					{
						t1.populate( v0 , v1, v2);
						builder.triangle( v0 , v1 ,v2 );
					}
					if ( t2.noSideExceedsLengthSquared( maxLenSquared ) ) {
						t2.populate( v0 , v1, v2 );
						builder.triangle( v0 , v1 ,v2 );                        	
					}
				} else {
					t1.populate( v0 , v1, v2 );
					builder.triangle( v0 , v1 ,v2 );

					t2.populate( v0 , v1, v2 );
					builder.triangle( v0 , v1 ,v2 );    
				}
			}
		}

		final Mesh mesh = builder.end();
		mesh.render( polyProgram , GL20.GL_TRIANGLES);
		polyProgram.end();
		mesh.dispose();			
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
				
//				if ( ( vertexCount + 2 ) >= 65535 ) 
//				{
//					// upload buffer
//					final int size = floatArrayBuilder.end();
//					vbo.setVertices( floatArrayBuilder.array , 0 , size );
//					
//					// render
//	    			// gl20.glDrawElements(GL20.GL_LINES , index , GL20.GL_UNSIGNED_SHORT , ibo.getBuffer() );        						
//	    			gl20.glDrawArrays(GL20.GL_LINES, 0 , vertexCount );
//	    			
//					// reset 
//					vertexCount = 0;
//					floatArrayBuilder.begin();
//				}
            }
        }

        if ( vertexCount != 0) 
        {
			// populate buffers
			final int size = floatArrayBuilder.end();
			vbo.setVertices( floatArrayBuilder.array , 0 , size );
			
			// render
			gl20.glDrawArrays(GL20.GL_LINES, 0 , vertexCount );
			// gl20.glDrawElements(GL20.GL_LINES , index , GL20.GL_UNSIGNED_SHORT , 0 );        						
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
		if ( polyProgram != null ) {
			polyProgram.dispose();
			polyProgram=null;
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

	private long frameCount = 0;
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