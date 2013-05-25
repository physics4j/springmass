package de.codesourcery.springmass.springmass;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import de.codesourcery.springmass.math.Vector4;

public class ClothPanel extends JFrame {

	private static final int X_RESOLUTION = 1000;
	private static final int Y_RESOLUTION = 1000;

	public static final int DELAY_MILLIS = 20;
	
	public static final boolean RENDER_ALL_LINES = false;
	
	public static final boolean RENDER_SPRINGS = false;
	
	public static final boolean RENDER_MASSES = false;
	
	private static final double MOUSE_DRAG_ZDEPTH = -100;	
	
	public static final double VERTICAL_RESTLENGTH_FACTOR = 1;
	public static final double HORIZONTAL_RESTLENGTH_FACTOR = 1;
	
	public static final boolean LIGHING = true;
	public static final Vector4 LIGHT_POS = new Vector4( X_RESOLUTION/3.5 , Y_RESOLUTION/2.5 , -200 );
	public static final Vector4 LIGHT_COLOR = new Vector4(.2,.2,0.8);

	public static final int COLUMNS = 633;	
	public static final int ROWS = 33;

	private final SimulationThread simulationThread;
	private final RenderPanel renderPanel;

	public static void main(String[] args) 
	{
		new ClothPanel();
	}

	public ClothPanel() 
	{
		// setup mass system
		final SpringMassSystem system = new SpringMassSystem();

		System.out.println("Point masses: "+(ROWS*COLUMNS));
		
		final Mass[][] masses = new Mass[COLUMNS][];
		for ( int i = 0 ; i < COLUMNS ; i++ ) {
			masses[i] = new Mass[ROWS];
		}

		double scaleX = X_RESOLUTION / (COLUMNS+COLUMNS*0.5);
		double scaleY = Y_RESOLUTION / (ROWS+ROWS*0.5);
		
		final double xOffset = scaleX;
		final double yOffset = scaleY;
		
		for ( int x = 0 ; x < COLUMNS ; x++ ) 
		{
			for ( int y = 0 ; y < ROWS ; y++ ) 
			{
				final Vector4 pos = new Vector4( xOffset + scaleX*x , yOffset + scaleY*y,0);
				final Mass m = new Mass( Color.red  , pos );
				if ( y == 0 ) {
					m.setFixed( true );
				}
				masses[x][y] = m;
				system.addMass(m);
			}
		}

		// connect masses horizontally
		final double horizRestLength = scaleX*HORIZONTAL_RESTLENGTH_FACTOR;
		for ( int y = 0 ; y < ROWS ; y++ ) 
		{
			for ( int x = 0 ; x < (COLUMNS-1) ; x++ ) 
			{
				system.addSpring( new Spring( masses[x][y] , masses[x+1][y] , horizRestLength , true ) );
			}
		}

		// connect masses vertically
		final double verticalRestLength = scaleY*VERTICAL_RESTLENGTH_FACTOR;
		for ( int x = 0 ; x < COLUMNS ; x++ ) 
		{
			for ( int y = 0 ; y < (ROWS-1) ; y++ ) 
			{
				system.addSpring( new Spring( masses[x][y] , masses[x][y+1] , verticalRestLength , true ) );
			}
		}	

		// cross-connect masses
		final double crossConnectRestLength = Math.sqrt( horizRestLength*horizRestLength + verticalRestLength*verticalRestLength);
		for ( int x = 0 ; x < (COLUMNS-1) ; x++ ) 
		{
			for ( int y = 0 ; y < (ROWS-1) ; y++ ) 
			{
				system.addSpring( new Spring( masses[x][y] , masses[x+1][y+1] , crossConnectRestLength , RENDER_ALL_LINES , Color.YELLOW ) );
				system.addSpring( new Spring( masses[x][y+1] , masses[x+1][y] , crossConnectRestLength , RENDER_ALL_LINES , Color.YELLOW ) );				
			}
		}	

		// connect cloth outline
		final double horizOutlineRestLength = 2 * horizRestLength;
		for ( int y = 0 ; y < ROWS ; y++ ) {
			for ( int x = 0 ; x < (COLUMNS-2) ; x++ ) 
			{
				system.addSpring( new Spring( masses[x][y] , masses[x+2][y] , horizOutlineRestLength , RENDER_ALL_LINES, Color.BLUE ) );
			}	
		}

		final double verticalOutlineRestLength = 2 * verticalRestLength;
		for ( int x = 0 ; x < COLUMNS ; x++ ) 
		{ 
			for ( int y = 0 ; y < (ROWS-2) ; y++ ) 
			{
				system.addSpring( new Spring( masses[x][y] , masses[x][y+2] , verticalOutlineRestLength , RENDER_ALL_LINES, Color.BLUE ) );
			}		
		}

		simulationThread = new SimulationThread(system);
		simulationThread.start();

		// setup panel
		renderPanel = new RenderPanel(system,masses,COLUMNS,ROWS);
		renderPanel.setPreferredSize( new Dimension(800,400 ) );
		getContentPane().add( renderPanel );
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setVisible( true );
	}

	protected final class SimulationThread extends Thread {

		private final Object LOCK = new Object();
		private final SpringMassSystem system;

		private boolean runSimulation = false;

		private long stepCounter=0;

		public SimulationThread(SpringMassSystem system) {
			setDaemon(true);
			this.system = system;
		}

		public void toggle() 
		{
			synchronized( LOCK ) 
			{
				if ( runSimulation ) 
				{
					System.out.println("Stopping simlation");
					runSimulation = false;
				} 
				else 
				{
					System.out.println("Starting simlation");
					runSimulation = true;
					LOCK.notifyAll();
				}
			}
		}

		@Override
		public void run() 
		{
			while(true) 
			{
				synchronized (LOCK) 
				{
					while( ! runSimulation ) 
					{
						try {
							System.out.println("Simulation thread sleeping");
							LOCK.wait();
							System.out.println("Simulation thread woke up");
						} catch (InterruptedException e) {
						}
					}
				}

				synchronized( system ) 
				{
					long time = -System.currentTimeMillis();
					system.step();
					time += System.currentTimeMillis();
					stepCounter++;
					if ( ( stepCounter % 100 ) == 0 ) {
						System.out.println("Calculation time: "+time+" ms");
					}
				}

				renderPanel.repaint();

				try {
					Thread.sleep(DELAY_MILLIS);
				} 
				catch (InterruptedException e) {
				}
			}
		}
	}

	protected final class RenderPanel extends JPanel {

		private final SpringMassSystem system;
		private final Mass[][] masses;
		private final int rows;
		private final int columns;
		
		private int frameCounter=0;
		
		private Mass selected;

		private final MouseAdapter mouseAdapter = new MouseAdapter() 
		{
			public void mousePressed(java.awt.event.MouseEvent e) 
			{
				if ( e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.BUTTON3 ) 
				{ 
					// left click
					final Vector4 mousePointer = viewToModel( e.getX() , e.getY() );
					final Mass nearest = system.getNearestMass( mousePointer , 5*5 );
					if ( e.getButton() == MouseEvent.BUTTON1 ) 
					{
						setSelected( nearest );
						RenderPanel.this.repaint();
					} 
					else 
					{
						if ( nearest != null ) 
						{
							nearest.setFixed( ! nearest.isFixed() );
							RenderPanel.this.repaint();
						}
					}
				} 
				else if ( e.getButton() == MouseEvent.BUTTON2 ) 
				{
					final Vector4 mousePointer = viewToModel( e.getX() , e.getY() );
					
					final Mass nearest = system.getNearestMass( mousePointer , 25*25 );
					
					if ( nearest != null ) 
					{
						final Vector4 edge = nearest.currentPosition.minus(new Vector4(1,0,0 ) );
						for ( Spring s : system.getIntersectingSprings( edge.x , nearest.currentPosition , 100 ) )
						{
							system.removeSpring( s);
						}
						RenderPanel.this.repaint();
					}
				}
			}
			
			private void setSelected(Mass m) 
			{
				if ( selected != null && selected != m ) 
				{
					selected.setSelected(false);
				}
				selected = m;
				if ( selected != null ) {
					selected.setSelected( true );
				}
			}

			public void mouseReleased(MouseEvent e) 
			{
				if ( e.getButton() == MouseEvent.BUTTON1 ) 
				{ 
					setSelected(null);
				}
			}

			public void mouseDragged(MouseEvent e) 
			{
				if ( selected != null ) 
				{
					final Vector4 newPos = viewToModel( e.getX() , e.getY() );
					newPos.z=MOUSE_DRAG_ZDEPTH;
					selected.setPosition( newPos );
					RenderPanel.this.repaint();
				}
			}
		};

		private final KeyAdapter keyListener = new KeyAdapter() 
		{
			public void keyTyped(java.awt.event.KeyEvent e) 
			{
				if ( e.getKeyChar() == 's' ) 
				{
					simulationThread.toggle();
				}
			}
		};
		
		public RenderPanel(SpringMassSystem system,Mass[][] masses,int columns,int rows) 
		{
			this.system = system;
			this.masses = masses;
			this.columns = columns;
			this.rows = rows;

			setFocusable(true);

			addKeyListener( keyListener );
			addMouseListener( mouseAdapter );
			addMouseMotionListener( mouseAdapter );
		}

		private Vector4 viewToModel(int x,int y) {

			double scaleX = getWidth() / (double) X_RESOLUTION;
			double scaleY = getHeight() / (double) Y_RESOLUTION;
			return new Vector4( x / scaleX , y / scaleY , 0 );
		}

		private Point modelToView(Vector4 vec) 
		{
			double scaleX = getWidth() / (double) X_RESOLUTION;
			double scaleY = getHeight() / (double) Y_RESOLUTION;
			return modelToView( vec , scaleX , scaleY ); 
		}

		private Point modelToView(Vector4 vec,double scaleX,double scaleY) 
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

		@Override
		protected void paintComponent(Graphics g) 
		{
			long time = -System.currentTimeMillis();
			super.paintComponent(g);
			synchronized( system ) 
			{
				final double scaleX = getWidth() / (double) X_RESOLUTION;
				final double scaleY = getHeight() / (double) Y_RESOLUTION;

				final int boxWidthUnits = 5;

				final int boxWidthPixels = (int) Math.round( boxWidthUnits * scaleX );
				final int boxHeightPixels = (int) Math.round( boxWidthUnits * scaleY );

				final int halfBoxWidthPixels = (int) Math.round( boxWidthPixels / 2.0 );
				final int halfBoxHeightPixels = (int) Math.round( boxHeightPixels / 2.0 );

				if ( LIGHING ) 
				{
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
					
					final int[] pointX = new int[3];
					final int[] pointY = new int[3];					
					for ( Triangle t : triangles ) 
					{
						Color color = t.calculateSurfaceColor( LIGHT_POS , LIGHT_COLOR );
						t.getViewCoordinates(pointX,pointY);
						g.setColor(color);
						g.fillPolygon(pointX,pointY,3); 
					}
				}
				
				if ( RENDER_MASSES ) 
				{
					for ( Mass m : system.masses ) 
					{
						final Point p = modelToView( m.currentPosition , scaleX , scaleY );
						if ( selected == m ) 
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
				}

				// render springs
				if ( RENDER_SPRINGS ) 
				{
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
				}
			}
			time += System.currentTimeMillis();
			frameCounter++;
			if ( ( frameCounter % 30 ) == 0 ) {
				System.out.println("Rendering time: "+time+" ms");
			}
		}
	}	
}
