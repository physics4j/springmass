package de.codesourcery.springmass.springmass;

import java.awt.Color;

import de.codesourcery.springmass.math.Vector4;

public class Spring {

	public static final double DAMPENING = 0.1;
	
	public final Mass m1;
	public final Mass m2;
	
	public final double coefficient=0.1;
	
	public final double restLen;
	
	public final double maxLength;
	
	public final boolean doRender;
	
	public final Color color;
	
	public Spring(Mass m1, Mass m2,double restLength) {
		this(m1,m2,restLength,false);
	}
	
	public Spring(Mass m1, Mass m2,double restLength,boolean doRender) { 
		this(m1,m2,restLength,doRender,Color.GREEN);
	}
	
	public Spring(Mass m1, Mass m2,double restLength,boolean doRender,Color color) 
	{
		if ( m1 == null ) {
			throw new IllegalArgumentException("m1 must not be null");
		}
		if ( m2 == null ) {
			throw new IllegalArgumentException("m2 must not be null");
		}
		this.restLen = restLength;
		this.maxLength = restLength*2;
		this.m1 = m1;
		this.m2 = m2;
		this.doRender = doRender;
		this.color = color;
	}
	
	public double distanceTo(Vector4 c) 
	{
		/*
So it can be written as simple as:
distance = |AB X AC| / sqrt(AB * AB)
Here X mean cross product of vectors, and * mean dot product of vectors. This applied in both 2 dimentional and three dimentioanl space.		 
		 */
		Vector4 ab = m2.currentPosition.minus( m1.currentPosition );
		Vector4 ac = c.minus( m1.currentPosition );
		return ab.crossProduct( ac ).length() / ab.length();
	}
	
	@Override
	public String toString() {
		return "Spring ( "+m1+" <-> "+m2+")";
	}
	
	@Override
	public int hashCode() 
	{
		return m1.hashCode() | m2.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) 
	{
		if ( obj instanceof Spring ) {
			Spring that = (Spring) obj;
			return (this.m1 == that.m1 && this.m2 == that.m2) ||
				   (this.m1 == that.m2 && this.m2 == that.m1 );
		} 
		return false;
	}
	
	public Vector4 calculateForce(Mass m) {
		
		final Vector4 lengthDelta;
		final Vector4 friction;
		if ( m == m2 ) { // m1 -> m2
			lengthDelta = m2.currentPosition.minus( m1.currentPosition );
			friction = m2.currentPosition.minus( m2.previousPosition );
		} else { // m2 -> m1
			lengthDelta = m1.currentPosition.minus( m2.currentPosition );
			friction = m1.currentPosition.minus( m1.previousPosition );
		}
		
		friction.multiplyInPlace( DAMPENING );
		
		final double force = -coefficient*(lengthDelta.length()-restLen);
		lengthDelta.multiplyInPlace( force );
		lengthDelta.minusInPlace( friction );
		return lengthDelta;
	}
}
