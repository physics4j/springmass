package de.codesourcery.springmass.springmass;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder.VertexInfo;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

import de.codesourcery.springmass.math.VectorUtils;

public class OpenGLRenderPanel implements IRenderPanel , Screen {

	private final Object SIMULATION_LOCK = new Object();

	// @GuardedBy( SIMULATION_LOCK )
	private SpringMassSystem system;

	// @GuardedBy( SIMULATION_LOCK )
	private SimulationParameters parameters;

	private int width;
	private int height;

	private final Object BUFFER_LOCK = new Object();

	private final PerspectiveCamera camera;
	
	private ShaderProgram shaderProgram;

	public OpenGLRenderPanel(int width,int height) throws IOException
	{
		this.camera = new PerspectiveCamera();
		this.camera.position.set( 110 , 30 , -200 );
		this.camera.far = 600;
		this.camera.near = 1;
		this.camera.fieldOfView = 90;
		this.camera.direction.set( 0, 0, 1 );
		this.camera.up.set( 0, 1, 0 );
		
		resize(width,height);
		
		try {
			shaderProgram = loadShader("default");
		} catch(IOException e) {
			throw new IOException("Failed to load 'default' shader: "+e.getMessage(),e);
		}
	}
	
	private static ShaderProgram loadShader(String name) throws IOException {
		
		final String vShaderClasspathPath= "/vertexshader/"+name+"_vertex.glsl";
		final String fShaderClasspathPath= "/fragmentshader/"+name+"_fragment.glsl";
		
		final String vshader = loadFromClasspath( vShaderClasspathPath );
		final String fshader = loadFromClasspath( fShaderClasspathPath );
		return new ShaderProgram( vshader , fshader );
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

		public Triangle() {
		}
		public void set(Vector3 p0,Vector3 p1,Vector3 p2) {
			this.p0 = p0;
			this.p1 = p1;
			this.p2 = p2;
		}
		
		@Override
		public String toString() {
			return "Triangle[ "+p0+" -> "+p1+" -> "+p2+" ]"; 
		}

		public void populate(VertexInfo v0,VertexInfo v1,VertexInfo v2) {
			
			v0.position.set( p0.x , p0.y , p0.z );
			v1.position.set( p1.x , p1.y , p1.z );
			v2.position.set( p2.x , p2.y , p2.z );

			// crappy normals , interpolate across triangles for better results
			Vector3 normal = getSurfaceNormal();
			v0.setNor( normal );
			v1.setNor( normal );
			v2.setNor( normal );
		}

		public boolean noSideExceedsLengthSquared(double lengthSquared) 
		{
			return p0.dst2( p1 ) <= lengthSquared && p0.dst2( p2 ) <= lengthSquared;
		}

		public Vector3 getSurfaceNormal() 
		{
			Vector3 v1 = new Vector3(p1).sub( p0 );
			Vector3 v2 = new Vector3(p2).sub( p0 );
			return v2.crs( v1 ).nor();
		}

		public Vector3 calculateLightVector(Vector3 lightPos) {
			return new Vector3(lightPos).sub(p0).nor();
		}

		public Color calculateSurfaceColor(Vector3 lightPos,Vector3 lightColor) 
		{
			Vector3 normal = getSurfaceNormal();
			Vector3 lightVector = calculateLightVector( lightPos );

			final float angle = Math.abs( normal.dot( lightVector ) );
			return VectorUtils.toColor( new Vector3(lightColor).scl( angle ) );
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
	
	private volatile int fpsCounter;

	private void render(SpringMassSystem system,SimulationParameters parameters,float currentAvgFPS) 
	{
		// clear display
		GL20 gl20 = Gdx.graphics.getGL20();
		gl20.glClearColor( 1 , 1, 1, 1 );
		gl20.glClear( GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT );

		gl20.glDisable( GL20.GL_DEPTH_TEST );
		gl20.glDisable( GL20.GL_CULL_FACE );
		
		fpsCounter++;
		if ( (fpsCounter%10) == 0 ) {
			System.out.println("fps: "+currentAvgFPS);
		}

		// g.drawString( "Avg. FPS: "+FPS_FORMAT.format( currentAvgFPS ) , 5, 15 );
		// g.drawString("Left-click to drag cloth | Right-click to pin/unpin particles | Set max. spring length > 0 to enable tearing"  , 5, getHeight()-15 );

		final double scaleX = getWidth() / (double) parameters.getXResolution();
		final double scaleY = getHeight() / (double) parameters.getYResolution();

		final int boxWidthUnits = 5;

		final int boxWidthPixels = (int) Math.round( boxWidthUnits * scaleX );
		final int boxHeightPixels = (int) Math.round( boxWidthUnits * scaleY );

		final int halfBoxWidthPixels = (int) Math.round( boxWidthPixels / 2.0 );
		final int halfBoxHeightPixels = (int) Math.round( boxHeightPixels / 2.0 );

		//        if ( parameters.isLightSurfaces() ) 
		//        {
		final int rows = parameters.getGridRowCount();
		final int columns = parameters.getGridColumnCount();

		final int triangleCount = rows*columns*2;
		final boolean checkArea = parameters.getMaxSpringLength() > 0;
		final double maxLenSquared = parameters.getMaxSpringLength()*parameters.getMaxSpringLength();

		shaderProgram.begin();
		
		/*
in vec4 vVertex; //ok
in vec4 vNormal; //ok

smooth out vec4 color;

uniform mat4 normalMatrix; // pok
uniform mat4 mvMatrix; // ok
uniform mat4 mvpMatrix; // ok

uniform vec4 diffuseColor; // ok
uniform vec4 vLightPosition; // ok	 
		 */

		shaderProgram.setUniformMatrix("mvpMatrix" , camera.combined );
		
//		shaderProgram.setUniformMatrix( "mvMatrix" , camera.view );
//		shaderProgram.setUniformMatrix( "normalMatrix", camera.invProjectionView );		
//		shaderProgram.setUniformf( "diffuseColor" , new Vector3(1,0,0) );
//		shaderProgram.setUniformf( "vLightPosition" , new Vector3(500,0,-1000) );
		
		final MeshBuilder builder = new MeshBuilder();

		VertexAttribute positionAttr = VertexAttribute.Position();
		positionAttr.alias = "vVertex";
		
		VertexAttribute normalAttr = VertexAttribute.Normal();
		normalAttr.alias = "vNormal";
		
		final VertexAttribute[] attributes = new VertexAttribute[] {
				positionAttr,
				normalAttr
		};

		builder.begin(  new VertexAttributes( attributes ) );

		VertexInfo v0 = new VertexInfo();
		VertexInfo v1 = new VertexInfo();
		VertexInfo v2 = new VertexInfo();

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

				t1.set(p0,p1,p2);
				t2.set(p1,p3,p2);	
				
				if ( checkArea ) 
				{
					if ( t1.noSideExceedsLengthSquared( maxLenSquared ) ) 
					{
						t1.populate( v0 , v1, v2);
						builder.triangle( v0 , v1 ,v2 );
						// triangles.add( t1 );
					}
					if ( t2.noSideExceedsLengthSquared( maxLenSquared ) ) {
						t2.populate( v0 , v1, v2);
						builder.triangle( v0 , v1 ,v2 );                        	
						// triangles.add( t2 );
					}
				} else {
					t1.populate( v0 , v1, v2);
					builder.triangle( v0 , v1 ,v2 );
					// triangles.add( t1 );

					t2.populate( v0 , v1, v2);
					builder.triangle( v0 , v1 ,v2 );    
					// triangles.add( t2 );
				}
				System.out.println("t1= "+t1);
				System.out.println("t2= "+t2);
				break;
			}
		}

		final Mesh mesh = builder.end();
		mesh.render( shaderProgram , GL20.GL_TRIANGLES);
		shaderProgram.end();
		mesh.dispose();		

		// sort by Z-coordinate and draw from back to front
		//            Collections.sort( triangles );
		//
		//            final int[] pointX = new int[3];
		//            final int[] pointY = new int[3];					
		//            for ( Triangle t : triangles ) 
		//            {
		//                Color color = t.calculateSurfaceColor( parameters.getLightPosition() , parameters.getLightColor() );
		//                t.getViewCoordinates(pointX,pointY);
		//                g.setColor(color);
		//                g.fillPolygon(pointX,pointY,3); 
		//            }
		//        }

		//        if ( parameters.isRenderMasses() ) 
		//        {
		//        	for ( int y = 0 ; y < parameters.getGridRowCount() ; y++ ) 
		//        	{
		//            	for ( int x = 0 ; x < parameters.getGridColumnCount() ; x++ ) 
		//            	{
		//            		final Mass m = system.massArray[x][y];
		//                    final Point p = modelToView( m.currentPosition , scaleX , scaleY );
		//                    if ( m.isSelected() ) 
		//                    {
		//                        g.setColor(Color.RED );
		//                        g.drawRect( p.x - halfBoxWidthPixels , p.y - halfBoxHeightPixels , boxWidthPixels , boxHeightPixels );
		//                        g.setColor(Color.BLUE);
		//                    } 
		//                    else 
		//                    {
		//                        if ( m.isFixed() ) {
		//                            g.setColor( Color.BLUE );
		//                            g.fillRect( p.x - halfBoxWidthPixels , p.y - halfBoxHeightPixels , boxWidthPixels , boxHeightPixels );								
		//                        } else {
		//                            g.setColor( m.color );
		//                            g.drawRect( p.x - halfBoxWidthPixels , p.y - halfBoxHeightPixels , boxWidthPixels , boxHeightPixels );								
		//                        }
		//                    }            		
		//            	}
		//        	}
		//        }

		// render springs
		//        if ( parameters.isRenderSprings() || parameters.isRenderAllSprings() ) 
		//        {
		//            g.setColor(Color.GREEN);
		//            for ( Spring s : system.getSprings() ) 
		//            {
		//                if ( s.doRender ) {
		//                    final Point p1 = modelToView( s.m1.currentPosition );
		//                    final Point p2 = modelToView( s.m2.currentPosition );
		//                    g.setColor( s.color );
		//                    g.drawLine( p1.x , p1.y , p2.x , p2.y );
		//                }
		//            }
		//        }

		gl20.glDisable( GL20.GL_CULL_FACE );            
		gl20.glDisable( GL20.GL_DEPTH_TEST );
	}

	// ==== libgdx ====

	@Override
	public void dispose() 
	{
		if ( shaderProgram != null ){
			shaderProgram.dispose();
			shaderProgram=null;
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
	public void resize(int width, int height) {
		this.width = width;
		this.height = height;
		camera.viewportHeight = height;
		camera.viewportWidth = width;
		camera.update(true);
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
		// TODO Auto-generated method stub

	}

	@Override
	public void addMouseListener(MouseListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addMouseMotionListener(MouseMotionListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPreferredSize(Dimension preferredSize) {
		resize(preferredSize.width ,preferredSize.height);
	}
}