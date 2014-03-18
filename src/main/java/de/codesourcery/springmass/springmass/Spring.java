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

import com.badlogic.gdx.math.Vector3;

public final class Spring {

    public final Mass m1;
    public final Mass m2;

    private final float coefficient;

    private final float restLen;

    public final boolean doRender;

    public final Color color;
    
    /*  double im1 = 1/m1.mass; 
     *  double im2 = 1/m2.mass; 
     *  double ratio = im1 / (im1 + im2);
     */
    private final float m1m2Ratio; // 
    
    public Vector3 force = new Vector3();

    public Spring createCopy(Mass newM1,Mass newM2 ) {
        return new Spring(newM1, newM2, restLen, doRender, color, coefficient);
    }
    
    public Spring(Mass m1, Mass m2,float restLength) {
        this(m1,m2,restLength,false);
    }

    public Spring(Mass m1, Mass m2,float restLength,boolean doRender) 
    { 
        this(m1,m2,restLength,doRender,Color.GREEN);
    }

    public Spring(Mass m1, Mass m2,float restLength,boolean doRender,Color color) 
    {
        this(m1, m2, restLength, doRender, color, 0.1f);
    }

    public double lengthSquared() {
        return m1.currentPosition.dst2(m2.currentPosition);
    }

    public Spring(Mass m1, Mass m2,float restLength,boolean doRender,Color color,float coefficient) 
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

        final float im1 = 1/m1.mass; 
        final float im2 = 1/m2.mass; 
        this.m1m2Ratio = im1 / (im1 + im2);
    }

    public double distanceTo(Vector3 c) 
    {
        /*
So it can be written as simple as:
distance = |AB X AC| / sqrt(AB * AB)
Here X mean cross product of vectors, and * mean dot product of vectors. This applied in both 2 dimentional and three dimentioanl space.		 
         */
        Vector3 ab = new Vector3(m2.currentPosition).sub( m1.currentPosition );
        Vector3 ac = new Vector3(c).sub( m1.currentPosition );
        return new Vector3(ab).crs( ac ).len() / ab.len();
    }

    @Override
    public String toString() {
        return "Spring ( "+m1+" <-> "+m2+")";
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if ( obj instanceof Spring) 
        {
            final Spring that = (Spring) obj;
            return this.m1 == that.m1 && this.m2 == that.m2;
        }
        return false;
    }
    
    @Override
    public int hashCode()
    {
        return this.m1.hashCode() | this.m2.hashCode();
    }

    public void remove() {
        m2.springs.remove( this );
        m1.springs.remove( this );
    }

    public void calcForce() 
    {
        force.set( m1.currentPosition );
        force.sub( m2.currentPosition );
        
        final float difference = (restLen - force.len()); 
        force.scl( m1m2Ratio * coefficient * difference );
    }	
}
