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

import de.codesourcery.springmass.math.Vector4;

public final class Spring {

    public final Mass m1;
    public final Mass m2;

    private final double coefficient;

    private final double restLen;

    public final boolean doRender;

    public final Color color;

    /*  double im1 = 1/m1.mass; 
     *  double im2 = 1/m2.mass; 
     *  double ratio = im1 / (im1 + im2);
     */
    private final double m1m2Ratio; // 
    
    public Vector4 force = new Vector4();

    public Spring createCopy(Mass newM1,Mass newM2 ) {
        return new Spring(newM1, newM2, restLen, doRender, color, coefficient);
    }
    
    public Spring(Mass m1, Mass m2,double restLength) {
        this(m1,m2,restLength,false);
    }

    public Spring(Mass m1, Mass m2,double restLength,boolean doRender) 
    { 
        this(m1,m2,restLength,doRender,Color.GREEN);
    }

    public Spring(Mass m1, Mass m2,double restLength,boolean doRender,Color color) 
    {
        this(m1, m2, restLength, doRender, color, 0.1);
    }

    public double lengthSquared() {
        return m1.currentPosition.minus(m2.currentPosition).lengthSquared();
    }

    public Spring(Mass m1, Mass m2,double restLength,boolean doRender,Color color,double coefficient) 
    {
        if ( m1 == null ) {
            throw new IllegalArgumentException("m1 must not be null");
        }
        if ( m2 == null ) {
            throw new IllegalArgumentException("m2 must not be null");
        }
        this.restLen = restLength;
        this.m1 = m1;
        this.m2 = m2;
        this.doRender = doRender;
        this.color = color;
        this.coefficient = coefficient;

        final double im1 = 1/m1.mass; 
        final double im2 = 1/m2.mass; 
        this.m1m2Ratio = im1 / (im1 + im2);
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

    public void remove() {
        m2.springs.remove( this );
        m1.springs.remove( this );
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

    public void calcForce() 
    {
        force.set( m1.currentPosition );
        force.minusInPlace( m2.currentPosition );
        
        final double difference = (restLen - force.length()); 
        force.multiplyInPlace( m1m2Ratio * coefficient * difference );
    }	
}
