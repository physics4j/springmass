package de.codesourcery.springmass.math;

import com.badlogic.gdx.math.Vector3;

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
	
	public Vector3 toUnitVector() {
		
		final float x = (float) ( Math.sin( xzAngleInRad ) * Math.cos( xyAngleInRad ) );
		final float y = (float) ( Math.sin( xzAngleInRad ) * Math.sin( xyAngleInRad ) );
		final float z = (float)   Math.cos( xzAngleInRad ); 
		
		return new Vector3(x,y,z);
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