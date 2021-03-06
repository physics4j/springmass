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

import de.codesourcery.springmass.math.Vector4;

/**
 * Spherical coordinates.
 * 
 * @author tobias.gierke@code-sourcery.de
 */
public class SphericalCoordinates {

	private float xzAngleInRad=0;
	private float xyAngleInRad=0;

	public SphericalCoordinates() {
	}

	public SphericalCoordinates(float xzAngleInRad, float xyAngleInRad) 
	{
		this.xzAngleInRad = assertValidAngleInRad(xzAngleInRad);
		this.xyAngleInRad = assertValidAngleInRad(xyAngleInRad);
	}
	
	public void set(SphericalCoordinates other) {
		this.xzAngleInRad = other.xzAngleInRad;
		this.xyAngleInRad = other.xyAngleInRad;
	}
	
	public float getXYAngleInRad() {
		return xyAngleInRad;
	}
	
	public void setXYAngleInRad(float xyAngleInRad) {
		this.xyAngleInRad = assertValidAngleInRad( xyAngleInRad );
	}
	
	public void setXZAngleInRad(float xzAngleInRad) {
		this.xzAngleInRad = assertValidAngleInRad( xzAngleInRad );
	}
	
	public float getXZAngleInRad() {
		return xzAngleInRad;
	}
	
	public Vector4 toUnitVector() {
		
		final float x = (float) ( Math.sin( xzAngleInRad ) * Math.cos( xyAngleInRad ) );
		final float y = (float) ( Math.sin( xzAngleInRad ) * Math.sin( xyAngleInRad ) );
		final float z = (float)   Math.cos( xzAngleInRad ); 
		
		return new Vector4(x,y,z,1);
	}
	
	private float assertValidAngleInRad(float rad) {
		if ( rad <0 || rad >= 2*Math.PI ) {
			throw new IllegalArgumentException("Angle must be [0,2*PI) , was: "+rad);
		}
		return rad;
	}	
	
	public static float getMinXZAngleInRad(SphericalCoordinates coords1, SphericalCoordinates coords2) {
		return Math.min( coords1.xzAngleInRad , coords2.xzAngleInRad );
	}
	
	public static float getMaxXZAngleInRad(SphericalCoordinates coords1,SphericalCoordinates coords2) {
		return Math.max( coords1.xzAngleInRad , coords2.xzAngleInRad );
	}	
	
	public static float getMinXYAngleInRad(SphericalCoordinates coords1,SphericalCoordinates coords2) {
		return Math.min( coords1.xyAngleInRad , coords2.xyAngleInRad );
	}
	
	public static float getMaxXYAngleInRad(SphericalCoordinates coords1,SphericalCoordinates coords2) {
		return Math.max( coords1.xyAngleInRad , coords2.xyAngleInRad );
	}		
}