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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.codesourcery.springmass.math.Vector4;

public class Mass {
	
	public Vector4 currentPosition;
	public Vector4 previousPosition;	
	
	public final double mass;
	
	public final Color color;
	
	public final List<Spring> springs = new ArrayList<>();
	
	public final Vector4 force = new Vector4(0,0,0,1);
	
	private byte flags;
	
	public static enum Flag 
	{
		FIXED(0),
		SELECTED(1);
		
		private final byte mask;
		
		private Flag( int bit ) {
			this.mask = (byte) ( 1 << bit );
		}
		
		public boolean isSet(byte in) {
			return ( in & mask ) != 0;
		}
		
		public byte setOrClear(byte in, boolean set) {
			if ( set ) {
				return (byte) ( in | mask );
			} 
			return (byte) (in & ~mask );
		}
	}
	
	@Override
	public String toString() {
		return "Mass( "+currentPosition+" )";
	}
	
	public boolean hasSprings() {
		return ! springs.isEmpty();
	}
	
	public List<Spring> getLeftSprings() 
	{
		final List<Spring> result = new ArrayList<>();
		for ( Spring s : springs ) {
			final Mass candidate;
			if ( s.m1 == this ) {
				candidate = s.m2;
			} else {
				candidate = s.m1;
			}
			if ( candidate.currentPosition.x < this.currentPosition.x ) 
			{
				result.add( s );
			}
		}
		return result;
	}
	
	public void setPosition(Vector4 p) {
		this.currentPosition = new Vector4(p);
		this.previousPosition = new Vector4(p);
	}
	
	public double distanceTo(Mass other) {
		return currentPosition.distanceTo( other.currentPosition );
	}
	
	public void setFixed(boolean yesNo) {
		flags = Flag.FIXED.setOrClear( flags  , yesNo );
	}
	
	public void addSpring(Spring s) 
	{
		s.m1.springs.add( s );
		s.m2.springs.add( s );
	}
	
	public boolean isFixed() {
		return Flag.FIXED.isSet( flags );
	}
	
	public void setSelected(boolean yesNo) {
		flags = Flag.SELECTED.setOrClear( flags  , yesNo );
	}	
	
	public boolean isSelected() {
		return Flag.SELECTED.isSet( flags );
	}	
	
	public Mass(Color color,Vector4 position,double mass) {
		if (position == null) {
			throw new IllegalArgumentException("position must not be null");
		}
		this.color = color;
		this.mass = mass;
		setPosition(position);
	}

	public double squaredDistanceTo(Vector4 other) {
		return currentPosition.distanceTo( other );
	}

	public void removeSpring(Spring s) 
	{
		if ( s.m1 == this ) 
		{
			s.m2.springs.remove( s );
		} else {
			s.m1.springs.remove(s);
		}
		springs.remove( s );
	}
}