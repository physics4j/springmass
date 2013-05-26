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

import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.locks.ReentrantLock;

import de.codesourcery.springmass.math.Vector4;

public class SpringMassSystem {

	private static final ForkJoinPool pool = new ForkJoinPool();	
	private final ReentrantLock lock = new ReentrantLock();	

	private final Mass[][] massArray;
	public final List<Mass> masses = new ArrayList<>();
	public final List<Spring> springs = new ArrayList<>();

	private SimulationParameters params;

	public SpringMassSystem(SimulationParameters params,Mass[][] massArray) {
		this.params = params;
		this.massArray = massArray;
		for ( int x = 0 ; x < params.getGridColumnCount() ; x++ ) 
		{
			for ( int y = 0 ; y < params.getGridRowCount() ; y++ ) 
			{
				masses.add( massArray[x][y]);
			}
		}
	}

	public Mass[][] getMassArray() 
	{
		return massArray;
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

	public List<Spring> getSprings() {
		return springs;
	}

	public void addSpring(Spring s) {
		s.m1.addSpring( s );
		springs.add( s );
	}
	
	public void removeSpring(Spring s) 
	{
		for (Iterator<Spring> it = springs.iterator(); it.hasNext();) 
		{
			Spring actual = it.next();
			if ( actual == s ) 
			{
				it.remove();
				s.m1.removeSpring( s );
				return;
			}
		}
	}

	public void lock() {
		lock.lock();
	}

	public void unlock() {
		lock.unlock();
	}

	public void step() 
	{
		lock();
		try 
		{
			stepMultithreaded();
//			stepSingleThreaded();
		} finally {
			unlock();
		}
	}

	private void stepSingleThreaded() 
	{
		for ( Spring s : getSprings() ) 
		{
			Vector4 force = s.calcForce();
			s.m1.force.plusInPlace( force );
			s.m2.force.minusInPlace( force );
		}
		
		final Vector4 gravity = new Vector4(0,1,0).multiply(params.getGravity());
		applyForces( masses , gravity );
	}
	
	private void stepMultithreaded() 
	{
		final double maxSpringLength = params.getMaxSpringLength();
		final boolean checkLength = maxSpringLength > 0;
		for ( Iterator<Spring> it = springs.iterator() ; it.hasNext() ; )
		{
			final Spring s = it.next();
			if ( checkLength && s.length() > maxSpringLength && !(s.m1.isSelected() || s.m2.isSelected() ) )
			{
				it.remove();
				s.m1.removeSpring( s );
				continue;
			}
			
			Vector4 force = s.calcForce();
			s.m1.force.plusInPlace( force );
			s.m2.force.minusInPlace( force );
		}
		
		final Vector4 gravity = new Vector4(0,1,0).multiply(params.getGravity());
		MyTask task = new MyTask( masses , gravity );
		pool.submit( task );

		task.join();
	}	
	
	private void applyForces(List<Mass> masses,Vector4 gravity) 
	{
		final double deltaTSquared = params.getIntegratonTimeStep();
		
		for ( Mass mass : masses)
		{
			if ( mass.isFixed() || mass.isSelected() ) {
				continue;
			}
			final Vector4 sumForces = mass.force;
			
			// apply gravity
			sumForces.plusInPlace( gravity );

			final Vector4 tmp = new Vector4(mass.currentPosition);

			final Vector4 posDelta = mass.currentPosition.minus(mass.previousPosition);

			Vector4 dampening = posDelta.multiply( params.getSpringDampening() );
			sumForces.minusInPlace( dampening );
			
			sumForces.multiplyInPlace( 1.0 / (mass.mass*deltaTSquared) );
			posDelta.plusInPlace( sumForces );

			posDelta.clampMagnitudeInPlace( params.getMaxParticleSpeed() );
			mass.currentPosition.plusInPlace( posDelta );
			
			if ( mass.currentPosition.y > params.getYResolution() ) {
				mass.currentPosition.y = params.getYResolution();
			}
			mass.previousPosition = tmp;
			
			mass.force.set(0, 0, 0);
		}		
	}

	protected final class MyTask extends ForkJoinTask<Void> {

		private final List<Mass> masses;
		private final Vector4 gravity; 

		public MyTask(List<Mass> masses,Vector4 gravity) {
			this.masses = masses;
			this.gravity = gravity;
		}

		@Override
		public Void getRawResult() {
			return null;
		}

		@Override
		protected void setRawResult(Void value) {
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
				applyForces( masses , gravity );
				return;
			}

			final int split = len / 2;

			MyTask task1 = new MyTask( masses.subList( 0 , split ) , gravity );
			MyTask task2 = new MyTask( masses.subList( split , masses.size() ) , gravity ) ;
			invokeAll( task1,task2);
		}
	}

	public void removeLeftSprings(Mass nearest) 
	{
		lock();
		try {
			Point pos = findArrayPos(nearest);
			final List<Spring> springs = new ArrayList<>(nearest.springs);
			for ( Spring s : springs ) 
			{
				Mass other = s.m1 == nearest ? s.m2 : s.m1;
				Point otherPos = findArrayPos(other);
				if ( otherPos.x < pos.x ) {
					nearest.removeSpring( s );
				}
			}
			
			if ( pos.x+1 < params.getGridColumnCount() ) 
			{
				Mass right = massArray[pos.x+1][pos.y];
				final List<Spring> springs2 = new ArrayList<>(right.springs);
				for ( Spring s : springs2 ) 
				{
					Mass o;
					if ( s.m1 == right ) {
						o = s.m2;
					} else {
						o = s.m1;
					}
					if ( findArrayPos( o ).x < pos.x ) {
						o.removeSpring( s );
					}
				}
			}
		} 
		finally {
			unlock();
		}
	}
	
	private Point findArrayPos(Mass m) 
	{
		for ( int y = 0 ; y < params.getGridRowCount() ; y++) 
		{
			for (  int x = 0 ; x < params.getGridColumnCount() ; x++) {
				if ( massArray[x][y] == m) {
					return new Point(x,y);
				}
			}
		}
		throw new RuntimeException("Mass not found: "+m);
	}

}
