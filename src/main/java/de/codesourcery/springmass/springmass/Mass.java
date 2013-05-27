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
import java.util.List;

import de.codesourcery.springmass.math.Vector4;

public class Mass {
	
	public Vector4 currentPosition;
	public Vector4 previousPosition;	
	
	public final double mass;
	
	public final Color color;
	
	public final List<Spring> springs = new ArrayList<>();
	
	private byte flags;
	
	public static final byte FLAG_FIXED = 1<<0;
	public static final byte FLAG_SELECTED = 1<<1;
	
	@Override
	public String toString() {
		return "Mass( "+currentPosition+" )";
	}
	
	public void setPosition(Vector4 p) {
		this.currentPosition = new Vector4(p);
		this.previousPosition = new Vector4(p);
	}
	
	public double distanceTo(Mass other) {
		return currentPosition.distanceTo( other.currentPosition );
	}
	
	public void setFixed(boolean yesNo) 
	{
	    if ( yesNo ) {
	        this.flags |= FLAG_FIXED;
	    } else {
	        this.flags &= ~FLAG_FIXED;
	    }
	}
	
	public void addSpring(Spring s) 
	{
		s.m1.springs.add( s );
		s.m2.springs.add( s );
	}
	
	public boolean isFixed() {
		return (flags & FLAG_FIXED) != 0;
	}
	
	public void setSelected(boolean yesNo) {
        if ( yesNo ) {
            this.flags |= FLAG_SELECTED;
        } else {
            this.flags &= ~FLAG_SELECTED;
        }	    
	}	
	
	public boolean isSelected() {
        return (flags & FLAG_SELECTED) != 0;		
	}	
	
	public boolean hasFlags(int bitMask) {
	    return (flags & bitMask) != 0;
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
}