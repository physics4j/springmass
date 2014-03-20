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
package de.codesourcery.springmass.simulation;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import com.badlogic.gdx.math.Vector3;

public final class SpringMassSystem 
{
    private final ReentrantLock lock = new ReentrantLock();	
    private final ThreadPoolExecutor threadPool;

    protected final Mass[][] massArray;

    private final List<Spring> springs = new ArrayList<>();

    private final List<Spring> removedSprings = new ArrayList<>();
    
    private SimulationParameters params;

    private SpringMassSystem copiedFrom;
    
    // mapping ORIGINAL mass -> copied mass
    private IdentityHashMap<Mass,Mass> massMappings;
    
    private Random random;
    private final WindSimulator windSimulator;
    
    private final int forkJoinBatchCount;

    protected abstract class ParallelTaskCreator<T> 
    {
        public abstract Runnable createTask(Iterable<T> chunk,CountDownLatch taskFinishedLatch);
    }    
    
    protected interface GridIterator extends Iterator<Mass> 
    {
    	/**
    	 * Returns the mass right of the element returned by the last call to {@link #next()}.
    	 * 
    	 *  <p>If the current mass is the last/right-most one in given row, the current mass
    	 *  is being returned instead of the right neightbor.</-p> 
    	 * @return mass
    	 */
    	public Mass rightNeighbour();
    	
    	/**
    	 * Returns the mass below the element returned by the last call to {@link #next()}.
    	 * 
    	 *  <p>If the current mass is on the last row, the current mass is being returned instead of the bottom neightbor.</-p>     	 
    	 * @return mass 
    	 */    	
    	public Mass bottomNeighbour();
    }
    
	/**
	 * Iterable view of a rectangular slice of the mass array.
	 * 
	 * <p>Used to distribute work across multiple CPU cores.</p>
	 * 
	 * @author tobias.gierke@code-sourcery.de
	 */
    public static final class Slice implements Iterable<Mass> {
    	
    	protected final Mass[][] array;
    	
    	public final int xStart;
    	public final int yStart;
    	
    	public final int xEnd;
    	public final int yEnd;
    	
    	protected final boolean fetchNeighbours;
    	
    	protected final boolean isAtRightEdge; // used to determine whether it's safe to access the first column of the slice to the right of us 
    	protected final boolean isAtBottomEdge; // used to determine whether it's safe to access the first row of the slice to the below us
    	
    	/**
    	 * 
    	 * @param x0
    	 * @param y0
    	 * @param width
    	 * @param height
    	 * @param fetchNeighbours whether to also fetch the right and bottom neighbour of every element being traversed
    	 */
    	public Slice(Mass[][] massArray,int x0,int y0,int width,int height,boolean fetchNeighbours,boolean isAtRightEdge,boolean isAtBottomEdge) {
    		this.array = massArray;
    		this.xStart = x0;
    		this.yStart = y0;
    		this.xEnd = x0 + width;
    		this.yEnd = y0 + height;
    		this.fetchNeighbours = fetchNeighbours;
    		this.isAtRightEdge = isAtRightEdge;
    		this.isAtBottomEdge=isAtBottomEdge;
    	}
    	
    	@Override
    	public String toString() {
    		return "Slice[ ("+xStart+","+yStart+")->("+xEnd+","+yEnd+") , fetchNeighbours="+fetchNeighbours+" ]";
    	}
    	
    	public Iterator<Mass> iterator() 
    	{
    		if ( fetchNeighbours) 
    		{
        		return new GridIterator() 
        		{
        	    	private int x = xStart;
        	    	private int y = yStart;
        	    	
        	    	private Mass rightNeighbour;
        	    	private Mass bottomNeighbour;
        	    	
    				@Override
    				public boolean hasNext() 
    				{
    					return x < xEnd && y < yEnd;
    				}

    				@Override
    				public Mass next() 
    				{
    					final Mass result = array[x][y];
    					if ( isAtRightEdge ) {
    						rightNeighbour  = (x+1) < xEnd ? array[x+1][y] : result;
    					} else {
    						rightNeighbour  = array[x+1][y]; // TODO: hackish, another thread my manipulate this Mass instance concurrently...
    					}
    					
    					if ( isAtBottomEdge ) {
    						bottomNeighbour = (y+1) < yEnd ? array[x][y+1] : result;
    					} else {
    						bottomNeighbour = array[x][y+1];
    					}
    					
    					x++;
    					if ( x >= xEnd) {
    						x = xStart;
    						y++;
    					} 
    					return result;
    				}

    				@Override
    				public void remove() { throw new UnsupportedOperationException(); }

    				@Override
    				public Mass rightNeighbour() { return rightNeighbour; }

    				@Override
    				public Mass bottomNeighbour() { return bottomNeighbour; }
        		};    			
    		}
    		return new GridIterator() 
    		{
    	    	private int x = xStart;
    	    	private int y = yStart;
    	    	
				@Override
				public boolean hasNext() 
				{
					return x < xEnd && y < yEnd;
				}

				@Override
				public Mass next() 
				{
					final Mass result = array[x++][y];
					if ( x >= xEnd) {
						x = xStart;
						y++;
					} 
					return result;
				}

				@Override
				public void remove() { throw new UnsupportedOperationException(); }

				@Override
				public Mass rightNeighbour() { throw new UnsupportedOperationException("Wrong iterator type"); }

				@Override
				public Mass bottomNeighbour() { throw new UnsupportedOperationException("Wrong iterator type"); }
    		};
    	}
    }
    
    public SpringMassSystem createCopy() 
    {
        lock();
        try 
        {
            final Mass[][] arrayCopy = new Mass[ params.getGridColumnCount() ][];
            for ( int x = 0 ; x < params.getGridColumnCount() ; x++ ) 
            {
                arrayCopy[x] = new Mass[ params.getGridRowCount() ];
            }

            // clone particles
            final IdentityHashMap<Mass,Mass> massMappings = new IdentityHashMap<>();
            for ( int x = 0 ; x < params.getGridColumnCount() ; x++ ) 
            {
                for ( int y = 0 ; y < params.getGridRowCount() ; y++ ) 
                {        
                    final Mass mOrig = massArray[x][y];
                    final Mass mCopy = mOrig.createCopyWithoutSprings();
                    arrayCopy[x][y]=mCopy;
                    massMappings.put( mOrig , mCopy );
                }
            }

            final SpringMassSystem copy = new SpringMassSystem( this.params , arrayCopy , random  );
            copy.windSimulator.set( this.windSimulator );
            copy.massMappings = massMappings;
            copy.copiedFrom = this;

            // add springs
            for ( Spring spring : springs ) 
            {
                Mass copy1 = massMappings.get( spring.m1 );
                Mass copy2 = massMappings.get( spring.m2 );
                copy.addSpring( spring.createCopy( copy1 , copy2 ) );
            }
            
            return copy;
        } 
        finally {
            unlock();
        }
    }

    public void updateFromOriginal() 
    {
        lock();
        try 
        {
            final List<Spring> removed;            
            copiedFrom.lock();
            try 
            {
                // copy positions and flags
                final int columnCount = params.getGridColumnCount();
                final int rowCount = params.getGridRowCount();                
				for ( int x = 0 ; x < columnCount ; x++ ) 
                {
					for ( int y = 0 ; y < rowCount ; y++ ) 
                    {
                        final Mass original = copiedFrom.massArray[x][y];                
                        final Mass clone = this.massArray[x][y];
                        clone.copyPositionAndFlagsFrom( original );
                    }
                }  
                
                // get all springs that were removed since the last call
                // to updateFromOriginal() and immediately clear the list
                // so we don't process them again
                removed = new ArrayList<>( copiedFrom.removedSprings );
                copiedFrom.removedSprings.clear();                
            } 
            finally {
                copiedFrom.unlock();
            }
            
            final int len = removed.size();
            for ( int i = 0; i < len ; i++)
            {
				final Spring removedSpring = removed.get(i);
				Mass m1 = massMappings.get( removedSpring.m1 );
                Mass m2 = massMappings.get( removedSpring.m2 );
                final Spring copy = removedSpring.createCopy( m1 ,m2 );
                copy.remove();
                springs.remove( copy );
			}
        } finally {
            unlock();
        }
    }

    public SpringMassSystem(SimulationParameters params,Mass[][] massArray,Random random) 
    {
    	final int cpuCount = Runtime.getRuntime().availableProcessors();
    	this.forkJoinBatchCount = (int) Math.ceil( cpuCount * params.getForkJoinLoadFactor() );
    	System.out.println("Will process "+forkJoinBatchCount+" batches in parallel (cpu core count: "+cpuCount+", load factor: "+params.getForkJoinLoadFactor()+" )");
    	
    	this.random = random;
        this.params = params;
        
        this.windSimulator = new WindSimulator(random, params.getWindParameters() );
        
        this.massArray = massArray;
        
        final BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(this.forkJoinBatchCount * 2 );
        final ThreadFactory threadFactory = new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r)
            {
                final Thread t = new Thread(r,"calculation-thread");
                t.setDaemon(true);
                return t;
            }
        };
        threadPool = new ThreadPoolExecutor(cpuCount, cpuCount, 60, TimeUnit.SECONDS, workQueue, threadFactory, new ThreadPoolExecutor.CallerRunsPolicy() );
    }

    public void destroy() throws InterruptedException 
    {
        lock();
        try 
        {
            threadPool.shutdown();
            threadPool.awaitTermination(60,TimeUnit.SECONDS );
        } 
        finally 
        {
            unlock();
        }
    }

    public Mass[][] getMassArray() 
    {
        return massArray;
    }

    public Mass getNearestMass(Vector3 pos,double maxDistanceSquared) {

        Mass best = null;
        double closestDistance = Double.MAX_VALUE;
        for ( int y = 0 ; y < params.getGridRowCount() ; y++ ) 
        {
        	for ( int x = 0 ; x < params.getGridColumnCount() ; x++ ) {
        		final Mass m = massArray[x][y];
                double distance = m.squaredDistanceTo( pos ); 
                if ( best == null || distance < closestDistance ) 
                {
                    best = m;
                    closestDistance = distance; 
                }        		
        	}
        }
        return closestDistance > maxDistanceSquared ? null : best;
    }

    public List<Spring> getSprings() {
        return springs;
    }

    public void addSpring(Spring s) {
        s.m1.addSpring( s );
        springs.add( s );
    }

    public void lock() 
    {
        try 
        {
            if ( ! lock.tryLock( 5 , TimeUnit.SECONDS ) ) 
            {
                throw new RuntimeException("Thread "+Thread.currentThread().getName()+" failed to aquire lock after waiting 5 seconds");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void unlock() {
        lock.unlock();
    }
    
    private long stepCounter = 0;

    public void step() 
    {
        final Vector3 gravity = new Vector3(0,-1,0).scl(params.getGravity());
        final Vector3 zeroGravity = new Vector3(0,0,0);
        
        final boolean debugPerformance = params.isDebugPerformance();
        
        long time = 0;
        
        long timeSolve = 0;
        long timeRemove = 0;
        long timeApplyForces = 0;
        long timeCalcNormals = 0;
        lock();
        try 
        {
        	windSimulator.step();
        	
        	final boolean springsCanBreak = params.getMaxSpringLength() > 0;
            for ( int count = params.getIterationCount() ; count > 0 ; count--) 
            {
                // solve constraints
            	if ( debugPerformance ) {
            		time = System.currentTimeMillis(); 
            	}
                solveConstraints();
                if ( debugPerformance ) {
                	timeSolve += ( System.currentTimeMillis() - time );
                }

                // remove springs exceeding the max. length
                if ( springsCanBreak ) {
                	if ( debugPerformance ) {
                		time = System.currentTimeMillis(); 
                	}                	
                	removeBrokenSprings(springs);
                    if ( debugPerformance ) {
                    	timeRemove += ( System.currentTimeMillis() - time );
                    }                	
                }

                // apply spring forces to particles
            	if ( debugPerformance ) {
            		time = System.currentTimeMillis(); 
            	}                     
                if ( count == 1 )
                {
                	// calculate normals if wind is enabled , needed for force calculation
                	if ( params.getWindParameters().isEnabled()  ) 
                	{
	                	if ( debugPerformance ) {
	                		time = System.currentTimeMillis(); 
	                	}               
	                    calculateNormals();
	                    
	                    if ( debugPerformance ) {
	                    	timeCalcNormals += ( System.currentTimeMillis() - time );
	                    } 
                	}
                    
                   	applyForces(gravity , params.getWindParameters().isEnabled() );
                } else {
                	applyForces( zeroGravity , false ); 
                }
                if ( debugPerformance ) {
                	timeApplyForces += ( System.currentTimeMillis() - time );
                }                  
            }      
            
            // only calculate normals if wind is not enabled, otherwise the
            // normals have already been calculated when applying the wind forces
            if ( ! params.getWindParameters().isEnabled() ) 
            {
            	// calculate normals
            	if ( debugPerformance ) {
            		time = System.currentTimeMillis(); 
            	}               
                calculateNormals();
                
                if ( debugPerformance ) {
                	timeCalcNormals += ( System.currentTimeMillis() - time );
                }             	
            }
        } 
        finally {
            unlock();
            if ( debugPerformance ) {
            	stepCounter++;
            	if ( (stepCounter % 60 ) == 0 ) {
            		System.out.println("Time solve: "+timeSolve+" / timeRemove: "+timeRemove+" / timeApplyForces: "+timeApplyForces+" / timeCalcNormals: "+timeCalcNormals);
            	}
            }
        }
    }
    
    private void calculateNormals() 
    {
    	final ParallelTaskCreator<Mass> taskCreator = new ParallelTaskCreator<Mass>() {

			@Override
			public Runnable createTask(final Iterable<Mass> chunk,final CountDownLatch taskFinishedLatch) 
			{
				return new Runnable() {

					@Override
					public void run() 
					{
						try {
							calculateNormals( (Slice) chunk );
						} 
						finally {
							taskFinishedLatch.countDown();
						}
					}
				};
			}
    		
    	};
		forEachParallel(massArray, taskCreator , false );
	}

	private void calculateNormals(Slice slice) 
    {
		final Vector3 v1 = new Vector3();
		final Vector3 v2 = new Vector3();
		
    	final int xEnd = slice.xEnd;
    	final int yEnd = slice.yEnd;
    	
    	for ( int x = slice.xStart ; x < xEnd ; x++ ) 
    	{
    		for ( int y = slice.yStart ; y < yEnd ; y++ ) 
    		{
    			calculateAveragedNormal(x,y,xEnd,yEnd,v1,v2);
    		}
    	}
    }
    
	private void calculateAveragedNormal(int x,int y,int rowCount,int columnCount,Vector3 v1,Vector3 v2)
	{
		final Mass m = massArray[x][y];
		
		final Vector3 normal = m.normal;
		
		normal.set(0,0,0);
		
		final Vector3 position = m.currentPosition;
		
		if ( (x+1) < columnCount && (y+1) < rowCount ) {
			v1.set( massArray[x+1][y].currentPosition ).sub(position);
			v2.set( massArray[x][y+1].currentPosition ).sub(position);
			normal.add( v1.crs( v2 ) );
		}
		
		if ( (x+1) < columnCount && (y-1) >= 0 ) {
			v1.set( massArray[x][y-1].currentPosition ).sub(position);			
			v2.set( massArray[x+1][y].currentPosition ).sub(position);
			normal.add( v1.crs( v2 ) );
		}	
		
		if ( (x-1) >= 0 && (y-1) >= 0 ) {
			v1.set( massArray[x-1][y].currentPosition ).sub(position);
			v2.set( massArray[x][y-1].currentPosition ).sub(position);
			normal.add( v1.crs( v2 )  );
		}	
		
		if ( (x-1) >= 0 && (y+1) < rowCount ) {
			v1.set( massArray[x][y+1].currentPosition ).sub(position);			
			v2.set( massArray[x-1][y].currentPosition ).sub(position);
			normal.add( v1.crs( v2 ) );
		}		
		normal.nor();
	}    
    
    private void addWindForce(Vector3 result , Mass mass,Mass rightNeighbour,Mass bottomNeighbour, Vector3 normalizedWindForce,Vector3 windForce) 
    {
    	// calc. average surface normal by averaging three of the four corners
    	
    	float x = (mass.normal.x + rightNeighbour.normal.x + bottomNeighbour.normal.x ) / 3.0f;
    	float y = (mass.normal.y + rightNeighbour.normal.y + bottomNeighbour.normal.y ) / 3.0f;
    	float z = (mass.normal.z + rightNeighbour.normal.z + bottomNeighbour.normal.z ) / 3.0f;
    	
    	// calculate angle between wind direction and surface normal
    	
    	// dot product
    	x *= normalizedWindForce.x;
    	y *= normalizedWindForce.y;
    	z *= normalizedWindForce.z;
    	
        final float angle = Math.abs( x + y + z );
        
        // scale wind force by angle 
        result.x += (angle * windForce.x);
        result.y += (angle * windForce.y);
        result.z += (angle * windForce.z);
	}

	private void removeBrokenSprings(List<Spring> springs) 
    {
        double maxSpringLengthSquared = params.getMaxSpringLength();
        maxSpringLengthSquared *= maxSpringLengthSquared;
        
        for ( Iterator<Spring> it = springs.iterator() ; it.hasNext() ; )
        {
            final Spring s = it.next();
            if ( s.lengthSquared() > maxSpringLengthSquared && !(s.m1.isSelected() || s.m2.isSelected() ) )
            {
                it.remove();
                s.remove();
                removedSprings.add( s );
            }
        }
    }
    
    private void solveConstraints() 
    {
        final ParallelTaskCreator<Spring> creator = new ParallelTaskCreator<Spring>() {

            @Override
            public Runnable createTask(final Iterable<Spring> chunk,final CountDownLatch taskFinishedLatch)
            {
                return new Runnable() {

                    @Override
                    public void run()
                    {
                        try 
                        {
                            for ( Spring s : chunk ) 
                            {
                                s.calcForce();
                            }
                        } 
                        finally 
                        {
                            taskFinishedLatch.countDown();
                        }
                    }
                };
            }
        };
        forEachParallel( springs,  creator );        
    }    

    private void applyForces(final Vector3 gravity,final boolean applyWindForces) 
    {
        final ParallelTaskCreator<Mass> creator = new ParallelTaskCreator<Mass>() {

            @Override
            public Runnable createTask(final Iterable<Mass> chunk,final CountDownLatch taskFinishedLatch)
            {
                return new Runnable() {

                    @Override
                    public void run()
                    {
                        try {
                            applyForces( chunk , gravity , applyWindForces );
                        } finally {
                            taskFinishedLatch.countDown();
                        }
                    }
                };
            }
        };

        forEachParallel( massArray ,  creator , applyWindForces );
    }

    private void applyForces(final Iterable<Mass> masses,final Vector3 gravity,final boolean applyWindForces) 
    {
        final float deltaTSquared = params.getIntegrationTimeStep();

        final Vector3 windForce = new Vector3();
        windSimulator.getCurrentWindVector( windForce );
        
        final Vector3 normalizedWindForce = new Vector3( windForce );
        normalizedWindForce.nor();
        
        final float maxParticleSpeedSquared = params.getMaxParticleSpeed()  * params.getMaxParticleSpeed() ;
        
        final Vector3 sumForces = new Vector3();
        final Vector3 posDelta = new Vector3();
        final Vector3 dampening = new Vector3(); 
        
        final float minY = -params.getYResolution()*0.4f;
        
        final GridIterator it = (GridIterator) masses.iterator();
        while ( it.hasNext() )
        {
        	final Mass mass = it.next();
            if ( mass.hasFlags( Mass.FLAG_FIXED | Mass.FLAG_SELECTED ) ) {
                continue;
            }

            sumForces.set(0,0,0);
            
            // add wind force
            if ( applyWindForces ) 
            {
            	final Mass rightNeighbour = it.rightNeighbour();
				final Mass bottomNeighbour = it.bottomNeighbour();
				addWindForce(sumForces , mass, rightNeighbour , bottomNeighbour , normalizedWindForce, windForce);
            }
            
            // add forces from neighbour masses
            final int springCount = mass.springs.size();
            for ( int i = 0 ; i < springCount ; i++ ) {
            	final Spring s= mass.springs.get(i); 
                if ( s.m1 == mass ) {
                    sumForces.add( s.force );
                } else {
                    sumForces.sub( s.force );
                }
            }

            // apply gravity
            sumForces.add( gravity );

            posDelta.set(mass.currentPosition).sub(mass.previousPosition);
            
            dampening.set(posDelta).scl( params.getSpringDampening() );
            
            sumForces.sub( dampening );

            sumForces.scl( 1.0f / (mass.mass*deltaTSquared) );
            posDelta.add( sumForces );

            // clamp speed
            float len = posDelta.len2(); 
            if ( len > maxParticleSpeedSquared ) {
            	len = (float) Math.sqrt( len );
            	posDelta.scl( params.getMaxParticleSpeed() / len );
            }
            
            mass.previousPosition.set( mass.currentPosition );            
            mass.currentPosition.add( posDelta );

            if ( mass.currentPosition.y < minY) {
                mass.currentPosition.y = minY;
            }
        }
    }    

    private <T> void forEachParallel(List<T> data,ParallelTaskCreator<T> taskCreator) {

    	final int chunkSize = data.size() / forkJoinBatchCount;
        final List<List<T>> chunks = sliceList( data , chunkSize );
        final CountDownLatch latch = new CountDownLatch(chunks.size());
        for ( List<T> chunk : chunks )
        {
            threadPool.submit( taskCreator.createTask( chunk , latch ) );
        }

        try {
        	latch.await();
        } catch(Exception e) {
        	e.printStackTrace();
        }
    }
    
    private void forEachParallel(Mass[][] data,ParallelTaskCreator<Mass> taskCreator,boolean iteratorNeedsNeighbours) 
    {
        final List<Slice> slices = sliceArray( data , iteratorNeedsNeighbours );
        final CountDownLatch latch = new CountDownLatch( slices.size() );
        
        for ( Slice slice : slices )
        {
            threadPool.submit( taskCreator.createTask( slice , latch ) );
        }

        try {
        	latch.await();
        } catch(Exception e) {
        	e.printStackTrace();
        }
    }    
    
    private <T> List<Slice> sliceArray(final Mass[][] data,boolean iteratorNeedsNeighbours) 
    {
    	int horizSize = (int) Math.ceil( Math.sqrt( forkJoinBatchCount) );
    	if ( horizSize < 1 ) {
    		horizSize = 1;
    	}
    	
    	final int vertSize = horizSize;
    	
    	final int horizSlices = params.getGridColumnCount() / horizSize;
    	final int vertSlices = params.getGridRowCount() / vertSize;
    	
    	final int horizRest = params.getGridColumnCount() - horizSlices*horizSize;
    	final int vertRest = params.getGridRowCount() - vertSlices*vertSize;
    			
    	final List<Slice> result = new ArrayList<>();
    	
    	final int xEnd = horizSlices*horizSize;
    	final int yEnd = vertSlices*vertSize;
    	
		int x,y;
    	for ( y = 0; y < yEnd ; y+= vertSize ) 
    	{    	
    		final boolean isAtBottomEdge = (y+vertSize) >= yEnd;
	    	for ( x = 0 ; x < xEnd ; x+= horizSize ) 
	    	{
	    		boolean isAtRightEdge = (x+horizRest) >= xEnd;
	    		result.add( new Slice( data, x , y , horizSize , vertSize , iteratorNeedsNeighbours , isAtRightEdge , isAtBottomEdge ) );
	    	}
	    	if ( horizRest > 0 ) {
	    		result.add( new Slice( data, x , y , horizRest , vertSize , iteratorNeedsNeighbours , true , isAtBottomEdge  ) );
	    	}
    	}
    	if ( vertRest > 0 ) 
    	{
	    	for ( x = 0 ; x < xEnd ; x+= horizSize ) 
	    	{
	    		boolean isAtRightEdge = (x+horizRest) >= xEnd;
	    		result.add( new Slice( data, x , y , horizSize , vertRest , iteratorNeedsNeighbours , isAtRightEdge , true ) );
	    	}    
	    	if ( horizRest > 0 ) {
	    		result.add( new Slice( data, x , y , horizRest , vertRest , iteratorNeedsNeighbours , true , true ) );
	    	}	    	
    	}
    	return result;
    }

    private <T> List<List<T>> sliceList(final List<T> list,final int chunkSize) 
    {
        final List<List<T>> result = new ArrayList<List<T>>();
        final int listSize = list.size();

        for ( int currentIndex = 0 ; currentIndex < listSize ; currentIndex += chunkSize ) {
            int end=currentIndex+chunkSize;
            if ( end > listSize ) {
                end = listSize;
            }
            result.add( list.subList( currentIndex , end ) );
        }
        return result;
    }    
}