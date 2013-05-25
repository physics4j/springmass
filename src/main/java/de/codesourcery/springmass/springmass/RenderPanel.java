package de.codesourcery.springmass.springmass;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import de.codesourcery.springmass.math.Vector4;

public final class RenderPanel extends JPanel {

	private final Object SIMULATION_LOCK = new Object();
	
	// @GuardedBy( SIMULATION_LOCK )
	private SpringMassSystem system;
	
	// @GuardedBy( SIMULATION_LOCK )
	private SimulationParameters parameters;
	
	// @GuardedBy( SIMULATION_LOCK )
	private Mass[][] masses;
	
	// @GuardedBy( SIMULATION_LOCK )
	private int rows;
	
	// @GuardedBy( SIMULATION_LOCK )
	private int columns;
	
	private final Object BUFFER_LOCK = new Object();
	
	// @GuardedBy( BUFFER_LOCK )
	private Image backgroundBuffer;
	
	// @GuardedBy( BUFFER_LOCK )
	private Image activeBuffer;
	
	// @GuardedBy( SIMULATION_LOCK )
	private long frameCounter;	
	
	private final Runnable r = new Runnable() 
	{
		@Override
		public void run() {
			repaint();
		}
	};	

	public RenderPanel() 
	{
		setFocusable(true);
		addComponentListener( new ComponentAdapter() {

			@Override
			public void componentResized(ComponentEvent e) 
			{
				System.out.println("Panel size changed: "+getSize());
				synchronized(BUFFER_LOCK) 
				{
					activeBuffer = backgroundBuffer = null;
				}
			}
		});

	}
	
	private Image getActiveBuffer() 
	{
		synchronized(BUFFER_LOCK) 
		{
			if ( activeBuffer == null ) 
			{
				activeBuffer = createImage(getWidth(),getHeight());
			}
			return activeBuffer;
		}
	}
	
	private Image getBackgroundBuffer() 
	{
		synchronized(BUFFER_LOCK) 
		{
			if ( backgroundBuffer == null ) {
				backgroundBuffer = createImage(getWidth(),getHeight());
			}			
			return backgroundBuffer;
		}
	}	
	
	private void swapBuffers() 
	{
		synchronized(BUFFER_LOCK) 
		{
			Image bg = getBackgroundBuffer();
			Image fg = getActiveBuffer();
			this.activeBuffer = bg;
			this.backgroundBuffer = fg;
		}
	}
	
	public void setSimulator(Simulator simulator) 
	{
		synchronized (SIMULATION_LOCK) 
		{
			this.system = simulator.getSpringMassSystem();
			this.parameters = simulator.getSimulationParameters();
			this.masses = this.system.getMassArray();
			this.columns = simulator.getSimulationParameters().getGridColumns();
			this.rows = simulator.getSimulationParameters().getGridRows();
		}
	}

	public Vector4 viewToModel(int x,int y) {

		double scaleX = getWidth() / (double) parameters.getXResolution();
		double scaleY = getHeight() / (double) parameters.getYResolution();
		return new Vector4( x / scaleX , y / scaleY , 0 );
	}

	public Point modelToView(Vector4 vec) 
	{
		double scaleX = getWidth() / (double) parameters.getXResolution();
		double scaleY = getHeight() / (double) parameters.getYResolution();
		return modelToView( vec , scaleX , scaleY ); 
	}

	public Point modelToView(Vector4 vec,double scaleX,double scaleY) 
	{
		return new Point( (int) Math.round( vec.x * scaleX ) , (int) Math.round( vec.y * scaleY ) );
	}		
	
	protected final class Triangle implements Comparable<Triangle> {
		
		private final Vector4 p0;
		private final Vector4 p1;
		private final Vector4 p2;
		private final double z;
		
		public Triangle(Vector4 p0,Vector4 p1,Vector4 p2) {
			this.p0 = p0;
			this.p1 = p1;
			this.p2 = p2;
			this.z = (p0.z+p1.z+p2.z)/3;
		}
		
		@Override
		public int compareTo(Triangle o) 
		{
			if ( this.z > o.z ) {
				return -1;
			}
			if ( this.z < o.z ) {
				return 1;
			}
			return 0;
		}
		
		public Vector4 getSurfaceNormal() 
		{
			Vector4 v1 = p1.minus( p0 );
			Vector4 v2 = p2.minus( p0 );
			return v2.crossProduct( v1 ).normalize();
		}
		
		public Vector4 calculateLightVector(Vector4 lightPos) {
			return lightPos.minus(p0).normalize();
		}
		
		public void getViewCoordinates(int[] pointX,int[] pointY)
		{
			Point p = modelToView( p0 );
			pointX[0] = p.x;
			pointY[0] = p.y;
			
			p = modelToView( p1 );
			pointX[1] = p.x;
			pointY[1] = p.y;	
			
			p = modelToView( p2 );
			pointX[2] = p.x;
			pointY[2] = p.y;	
		}
		
		public Color calculateSurfaceColor(Vector4 lightPos,Vector4 lightColor) 
		{
			Vector4 normal = getSurfaceNormal();
			Vector4 lightVector = calculateLightVector( lightPos );
			
			final double angle = Math.abs( normal.dotProduct( lightVector ) );
			return lightColor.multiply( angle ).toColor();
		}
	}		
	
	public void doRender() 
	{
		render( getBackgroundBuffer() );
		swapBuffers();
		SwingUtilities.invokeLater( r );
	}
	
	private void render(Image image) 
	{
		long setupTime = 0;
		long renderingTime = 0;
		
		final Graphics g = image.getGraphics();
		
		// clear image
		g.setColor( getBackground() );
		g.fillRect( 0 , 0 , image.getWidth(null) , image.getHeight(null) );
		
		synchronized( SIMULATION_LOCK ) 
		{
			final double scaleX = getWidth() / (double) parameters.getXResolution();
			final double scaleY = getHeight() / (double) parameters.getYResolution();

			final int boxWidthUnits = 5;

			final int boxWidthPixels = (int) Math.round( boxWidthUnits * scaleX );
			final int boxHeightPixels = (int) Math.round( boxWidthUnits * scaleY );

			final int halfBoxWidthPixels = (int) Math.round( boxWidthPixels / 2.0 );
			final int halfBoxHeightPixels = (int) Math.round( boxHeightPixels / 2.0 );

			if ( parameters.isLightSurfaces() ) 
			{
				long time = -System.currentTimeMillis();
				final List<Triangle> triangles = new ArrayList<>( rows*columns*2 );
				for ( int y = 0 ; y < rows-1 ; y++) 
				{
					for ( int x = 0 ; x < columns-1 ; x++) 
					{
						Vector4 p0 = masses[x][y].currentPosition;
						Vector4 p1 = masses[x+1][y].currentPosition;
						Vector4 p2 = masses[x][y+1].currentPosition;
						Vector4 p3 = masses[x+1][y+1].currentPosition;
						
						triangles.add( new Triangle(p0,p1,p2) );
						triangles.add( new Triangle(p1,p3,p2) );
					}
				}
				
				// sort by Z-coordinate and draw from back to front
				Collections.sort( triangles );
				
				time += System.currentTimeMillis();
				setupTime += time;
				
				time = -System.currentTimeMillis();
				
				final int[] pointX = new int[3];
				final int[] pointY = new int[3];					
				for ( Triangle t : triangles ) 
				{
					Color color = t.calculateSurfaceColor( parameters.getLightPosition() , parameters.getLightColor() );
					t.getViewCoordinates(pointX,pointY);
					g.setColor(color);
					g.fillPolygon(pointX,pointY,3); 
				}
				time += System.currentTimeMillis();
				renderingTime += time;				
			}
			
			if ( parameters.isRenderMasses() ) 
			{
				long time = -System.currentTimeMillis();				
				for ( Mass m : system.masses ) 
				{
					final Point p = modelToView( m.currentPosition , scaleX , scaleY );
					if ( m.isSelected() ) 
					{
						g.setColor(Color.RED );
						g.drawRect( p.x - halfBoxWidthPixels , p.y - halfBoxHeightPixels , boxWidthPixels , boxHeightPixels );
						g.setColor(Color.BLUE);
					} 
					else 
					{
						if ( m.isFixed() ) {
							g.setColor( Color.BLUE );
							g.fillRect( p.x - halfBoxWidthPixels , p.y - halfBoxHeightPixels , boxWidthPixels , boxHeightPixels );								
						} else {
							g.setColor( m.color );
							g.drawRect( p.x - halfBoxWidthPixels , p.y - halfBoxHeightPixels , boxWidthPixels , boxHeightPixels );								
						}
					}
				}
				time += System.currentTimeMillis();
				renderingTime += time;					
			}

			// render springs
			if ( parameters.isRenderSprings() ) 
			{
				long time = -System.currentTimeMillis();	
				g.setColor(Color.GREEN);
				for ( Spring s : system.getSprings() ) 
				{
					if ( s.doRender ) {
						final Point p1 = modelToView( s.m1.currentPosition );
						final Point p2 = modelToView( s.m2.currentPosition );
						g.setColor( s.color );
						g.drawLine( p1.x , p1.y , p2.x , p2.y );
					}
				}
				time += System.currentTimeMillis();
				renderingTime += time;					
			}
			
			frameCounter++;
			if ( (frameCounter%30) == 0 ) {
				System.out.println("Rendering time breakdown: Setup time: "+setupTime+" ms / Drawing: "+renderingTime);
			}
		} // END: Critical section		
	}

	@Override
	protected void paintComponent(Graphics g) 
	{
		g.drawImage( getActiveBuffer() , 0 , 0 , getWidth() , getHeight() , null );
	}
}