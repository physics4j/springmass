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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

import de.codesourcery.springmass.math.Vector4;

public class SpringMassSystem {

	private static final ForkJoinPool pool = new ForkJoinPool();	
	
	public List<Mass> masses = new ArrayList<>();

	private SimulationParameters params;
	
	public SpringMassSystem(SimulationParameters params) {
		this.params = params;
	}
	
	public void addMass(Mass m) 
	{
		this.masses.add( m );
	}
	
	public Mass getNearestMass(Vector4 pos,double maxDistanceSquared) {
		
		Mass best = null;
		double closestDistance = Double.MAX_VALUE; 
		for ( Mass m : masses ) 
		{
			double distance = m.squaredDistanceTo( pos ); 
			if ( best == null || distance < closestDistance ) 
			{
				best = m;
				closestDistance = distance; 
			}
		}
		return closestDistance > maxDistanceSquared ? null : best;
	}
	
	public Set<Spring> getIntersectingSprings(double xCoordinate,Vector4 point,double maxDistance) 
	{
		final Set<Spring> result = new HashSet<>();
		for ( Mass m : masses ) 
		{
			for ( Spring s : m.springs ) 
			{
				if ( ( s.m1.currentPosition.x <= xCoordinate && s.m2.currentPosition.x >= xCoordinate ) ||
					   s.m2.currentPosition.x <= xCoordinate && s.m1.currentPosition.x >= xCoordinate ) 
				{  						
					if ( s.distanceTo( point) <= maxDistance) {
						result.add( s );
					}
				}
			}
		}
		return result;
	}
	
	public Set<Spring> getSprings() {
		
		final Set<Spring> result = new HashSet<Spring>();
		for ( Mass m : masses ) {
			result.addAll( m.springs );
		}
		return result;
	}
	
	public synchronized void removeSpring(Spring s) 
	{
		s.m1.springs.remove( s );
		s.m2.springs.remove( s );
	}
	
	public void addSpring(Spring s) {
		s.m1.addSpring( s );
	}
	
	public synchronized void step() 
	{

		MyTask task = new MyTask( masses );
		pool.submit( task );
		
		final List<TaskResult> results = task.join();
		
		// 
		
		/* Apply forces.
		 * 
		 * F = total of forces acting on this point
		 * T = Time step to update over
		 * X0 is the previous position, X1 is the current position
		 * XT = X1
		 * X1 += (X1-X0) + F/M*T*T
		 * X0 = XT		 
		 */
		final double deltaT = 6;
		final double deltaTSquared = deltaT*deltaT;		
		Vector4 gravity = new Vector4(0,1,0).multiply(9.81);
		for ( TaskResult entry : results )
		{
		   final Mass mass = entry.m;
		   
		   final Vector4 sumForces = entry.sumForces;
		   // apply gravity
		   sumForces.plusInPlace( gravity );
		   
		   final Vector4 tmp = new Vector4(mass.currentPosition);
		   
		   final Vector4 posDelta = mass.currentPosition.minus(mass.previousPosition);
		   
		   sumForces.multiplyInPlace( 1.0 / (mass.mass*deltaTSquared) );
		   posDelta.plusInPlace( sumForces );
		   
		   posDelta.clampMagnitudeInPlace( params.getMaxParticleSpeed() );
		   mass.currentPosition.plusInPlace( posDelta );
		   mass.previousPosition = tmp;
		}
	}
	
	private void stepSingleThreaded() 
	{
		final IdentityHashMap<Mass, Vector4> newForces=new IdentityHashMap<>();
		
		for ( Mass m : masses ) 
		{
			if ( ! m.isFixed() && ! m.isSelected() ) 
			{
				final Vector4 internalForces = m.calculateNeighbourForces();
				newForces.put( m , internalForces );
			}
		}
		
		// 
		
		/* Apply forces.
		 * 
		 * F = total of forces acting on this point
		 * T = Time step to update over
		 * X0 is the previous position, X1 is the current position
		 * XT = X1
		 * X1 += (X1-X0) + F/M*T*T
		 * X0 = XT		 
		 */
		final double deltaT = 6;
		final double deltaTSquared = deltaT*deltaT;
		
		Vector4 gravity = new Vector4(0,1,0).multiply(9.81);
		for ( Entry<Mass, Vector4> entry : newForces.entrySet() ) 
		{
		   final Mass mass = entry.getKey();
		   
		   final Vector4 sumForces = entry.getValue();
		   // apply gravity
		   sumForces.plusInPlace( gravity );
		   
		   final Vector4 tmp = new Vector4(mass.currentPosition);
		   
		   final Vector4 posDelta = mass.currentPosition.minus(mass.previousPosition);
		   
		   sumForces.multiplyInPlace( 1.0 / (mass.mass*deltaTSquared) );
		   posDelta.plusInPlace( sumForces );
		   
		   posDelta.clampMagnitudeInPlace( params.getMaxParticleSpeed() );
		   mass.currentPosition.plusInPlace( posDelta );
		   mass.previousPosition = tmp;
		}
	}
	
	protected static final class TaskResult 
	{
		public final Mass m;
		public final Vector4 sumForces;
		protected TaskResult(Mass m, Vector4 sumForces) {
			this.m = m;
			this.sumForces = sumForces;
		}
		
	}
	
	protected final class MyTask extends ForkJoinTask<List<TaskResult>> {

		private List<TaskResult> result= new ArrayList<>();
		private final List<Mass> masses;
		
		public MyTask(List<Mass> masses) {
			this.masses = masses;
		}
		
		@Override
		public List<TaskResult> getRawResult() {
			return result;
		}

		@Override
		protected void setRawResult(List<TaskResult> value) {
		}
		
	    protected final boolean exec() {
	        compute();
	        return true;
	    }
	    
	    /**
	     * The main computation performed by this task.
	     */
	    protected void compute() 
	    {
	    	final int len = masses.size();
	        if (len < params.getForkJoinBatchSize() ) 
	        {
	            computeDirectly();
	            return;
	        }
	        
	        final int split = len / 2;
	        
	        MyTask task1 = new MyTask( masses.subList( 0 , split ) );
	        MyTask task2 = new MyTask( masses.subList( split , masses.size() ) ) ;
            invokeAll( task1,task2);
            result.addAll( task1.getRawResult() );
            result.addAll( task2.getRawResult() );
	    }

		private void computeDirectly() 
		{
			for ( Mass m : masses ) 
			{
				if ( ! m.isFixed() && ! m.isSelected() ) 
				{
					final Vector4 internalForces = m.calculateNeighbourForces();
					result.add( new TaskResult( m , internalForces ) );
				}
			}
		}
	}
	
}
