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
package de.codesourcery.springmass.springmass;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import de.codesourcery.springmass.math.Vector4;

public final class RenderPanel extends Canvas {

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
    private volatile boolean bufferCreated = false;
    
    private final RenderThread renderThread = new RenderThread();

    protected final class RenderThread extends Thread 
    {
        private volatile int delay=20;
        private volatile boolean debugPerformance=false;        
        
        private final CountDownLatch latch=new CountDownLatch(1);
        private volatile boolean terminate;
        
        private final Object COUNTER_LOCK = new Object();
        
        // @GuardedBy( COUNTER_LOCK )
        private long frameCounter=0;
        
        // @GuardedBy( COUNTER_LOCK )        
        private long minTime=Long.MAX_VALUE;
        
        // @GuardedBy( COUNTER_LOCK )        
        private long sumTime;        
        
        // @GuardedBy( COUNTER_LOCK )        
        private long maxTime=Long.MIN_VALUE;        

        public RenderThread() 
        {
            setDaemon(true);
        }
        
        public void parametersChanged() 
        {
            this.delay = parameters.getFrameSleepTime();
            this.debugPerformance = parameters.isDebugPerformance();
            
            synchronized (COUNTER_LOCK) 
            {
                minTime=Long.MAX_VALUE;
                maxTime=Long.MIN_VALUE;                   
                frameCounter=0;
                sumTime=0;
            }
        }
        
        public void shutdown() throws InterruptedException 
        {
            terminate = true;
            latch.await();
        }

        @Override
        public void run()
        {
            try 
            {
                while ( ! terminate ) 
                {
                    long time = -System.currentTimeMillis();
                    boolean rendered = false;
                    try 
                    {
                        rendered = doRender();
                    } 
                    catch(Exception e) 
                    {
                        e.printStackTrace();
                    }
                    time += System.currentTimeMillis();
                    
                    if ( rendered ) 
                    {
                        synchronized (COUNTER_LOCK) 
                        {
                            frameCounter++;
                            
                            minTime = Math.min(time,minTime);
                            maxTime = Math.max(time,maxTime);
                            sumTime += time;
    
                            if ( debugPerformance && (frameCounter%30) == 0 ) 
                            {
                                final float avgTime = sumTime / (float) frameCounter;
                                
                                final float maxFps = 1000.0f / minTime;
                                final float minFps = 1000.0f / maxTime;
                                final float avgFps = 1000.0f / avgTime;
                                
                                System.out.println("frames "+frameCounter+" , current "+time+" ms / "+
                                		" min: "+minTime+" ms / avg: "+avgTime+" ms / max: "+maxTime+" ms (FPS: "+minFps+" / "+avgFps+" / "+maxFps+" )");
                            }
                        }
                    }

                    // sleep some time
                    try { Thread.sleep(delay); } 
                    catch(Exception e) 
                    {
                        e.printStackTrace();
                    }    
                    
                }
            } 
            finally {
                latch.countDown();
            }
        }    
    };


    
    public RenderPanel() 
    {
        setFocusable(true);
        addComponentListener( new ComponentAdapter() {

            @Override
            public void componentShown(ComponentEvent e) 
            {
                synchronized(BUFFER_LOCK) 
                {
                    createBufferStrategy(3);
                    bufferCreated = true;
                }
            }
            
            @Override
            public void componentResized(ComponentEvent e) 
            {
                synchronized(BUFFER_LOCK) 
                {
                    createBufferStrategy(3);
                    bufferCreated = true;
                }
            }
        });
    }

    public void setSimulator(Simulator simulator) 
    {
        synchronized (SIMULATION_LOCK) 
        {
            this.system = simulator.getSpringMassSystem();
            this.parameters = simulator.getSimulationParameters();
            this.masses = this.system.getMassArray();
            this.columns = simulator.getSimulationParameters().getGridColumnCount();
            this.rows = simulator.getSimulationParameters().getGridRowCount();
            this.renderThread.parametersChanged();
        }
    }
    
    public void addTo(Container container) 
    {
        container.add( this );
        
        if ( ! this.renderThread.isAlive() ) 
        {
            this.renderThread.start();
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

        public boolean noSideExceedsLengthSquared(double lengthSquared) 
        {
            return p0.distanceSquaredTo( p1 ) <= lengthSquared && p0.distanceSquaredTo( p2 ) <= lengthSquared;
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

    private boolean doRender() 
    {
        synchronized( BUFFER_LOCK ) 
        {
            if ( !bufferCreated ) 
            {
                return false;
            }
            BufferStrategy strategy = getBufferStrategy();
            Graphics graphics = strategy.getDrawGraphics();
            try {
                render( graphics );
            } finally {
                graphics.dispose();
            }
            strategy.show();
        }
        Toolkit.getDefaultToolkit().sync();
        return true;
    }

    private void render(Graphics g) 
    {
        long setupTime = 0;
        long renderingTime = 0;

        // clear image
        g.setColor( getBackground() );
        g.fillRect( 0 , 0 , getWidth() , getHeight() );

        synchronized( SIMULATION_LOCK ) 
        {
            system.lock();
            try 
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
                    final boolean checkArea = parameters.getMaxSpringLength() > 0;
                    final double maxLenSquared = parameters.getMaxSpringLength()*parameters.getMaxSpringLength();

                    for ( int y = 0 ; y < rows-1 ; y++) 
                    {
                        for ( int x = 0 ; x < columns-1 ; x++) 
                        {
                            Mass m0 = masses[x][y];
                            Mass m1 = masses[x+1][y];
                            Mass m2 = masses[x][y+1];
                            Mass m3 = masses[x+1][y+1];

                            Vector4 p0 = m0.currentPosition;
                            Vector4 p1 = m1.currentPosition;
                            Vector4 p2 = m2.currentPosition;
                            Vector4 p3 = m3.currentPosition;

                            Triangle t1 = new Triangle(p0,p1,p2);
                            Triangle t2 = new Triangle(p1,p3,p2);							
                            if ( checkArea ) {
                                if ( t1.noSideExceedsLengthSquared( maxLenSquared ) ) {
                                    triangles.add( t1 );
                                }
                                if ( t2.noSideExceedsLengthSquared( maxLenSquared ) ) {
                                    triangles.add( t2 );
                                }
                            } else {
                                triangles.add( t1 );
                                triangles.add( t2 );
                            }
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
                if ( parameters.isRenderSprings() || parameters.isRenderAllSprings() ) 
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

            } finally {
                system.unlock();
            }
        } // END: Critical section		
    }	
}