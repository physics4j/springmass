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
package de.codesourcery.springmass.math;

import java.awt.Color;
import java.text.DecimalFormat;

public final class Vector4 
{
    public double x;
    public double y;
    public double z;
    public double w;
    
    public static final Vector4 UP =        new Vector4( 0, 1, 0); // +Y
    public static final Vector4 DOWN =      new Vector4( 0,-1, 0); // -Y
    
    public static final Vector4 LEFT =      new Vector4(-1, 0, 0); // -X
    public static final Vector4 RIGHT =     new Vector4( 1, 0, 0); // +X
    
    public static final Vector4 INTO_VIEWPLANE =  new Vector4( 0, 0,-1); // -Z
    public static final Vector4 OUTOF_VIEWPLANE = new Vector4( 0, 0,1); // +Z      
    
    public Vector4(Vector4 input) 
    {
        this.x = input.x;
        this.y = input.y;
        this.z = input.z;
        this.w = input.w;
    }
    
    @Override
    public boolean equals(Object obj) 
    {
    	if ( obj != null && obj.getClass() == Vector4.class ) 
    	{
    		Vector4 o = (Vector4) obj;
    		return this.x == o.x && this.y == o.y && this.z == o.z && this.w == o.w;
    	}
    	return false;
    }
    
    public void copyInto(Vector4 other) {
    	other.x = x;
    	other.y = y;
    	other.z = z;
    	other.w = w;
    }
    
    public boolean equals(Object obj,double epsilon) 
    {
    	if ( obj != null && obj.getClass() == Vector4.class ) 
    	{
    		Vector4 o = (Vector4) obj;
    		return Math.abs( this.x - o.x ) < epsilon &&
    				Math.abs( this.y - o.y ) < epsilon &&
    				Math.abs( this.z - o.z ) < epsilon &&
    				Math.abs( this.w - o.w ) < epsilon;
    	}
    	return false;
    }    
    
    public static Vector4 valueOf(Color c) 
    {
    	return new Vector4( c.getRed() / 255.0 , c.getGreen() / 255.0 , c.getBlue() / 255.0 );
    }
    
    public int toRGB() 
    {
    	int r = (int) Math.max( 0 , Math.min(r()*255f,255) );
    	int g = (int) Math.max( 0 , Math.min(g()*255f,255) );
    	int b = (int) Math.max( 0 , Math.min(b()*255f,255) );
        int color = r << 16 | g << 8 | b;
        return color;
    }
    
    public Color toColor() {
    	return new Color( toRGB() );
    }
    
    public Vector4() {
    }
    
    public Vector4(double[] data) {
        this.x=data[0];
        this.y=data[1];
        this.z=data[2];
        this.w=data[3];
    }    
    
    public void setData(double[] data,int offset) {
        this.x = data[offset];
        this.y = data[offset+1];
        this.z = data[offset+2];
        this.w = data[offset+3];
    }
    
    public void copyFrom(Vector4 other)
    {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
        this.w = other.w;
    }    
    
    public void set(double x,double y,double z) {
    	this.x = x;
    	this.y = y;
    	this.z = z;
    }
    
    public void copyInto(double[] array,int startingOffset) 
    {
        array[startingOffset] = x;
        array[startingOffset+1] = y;
        array[startingOffset+2] = z;
        array[startingOffset+3] = w;
    }
    
    public Vector4(double[] data,int offset) 
    {
        this.x = data[offset];
        this.y = data[offset+1];
        this.z = data[offset+2];
        this.w = data[offset+3];
    }
    
    public Vector4 flip() 
    {
    	return new Vector4(-x,-y,-z,w);
    }
    
    public void flipInPlace() 
    {
    	this.x = -x;
    	this.y = -y;
    	this.z = -z;
    }    
    
    public boolean isEquals(Vector4 other) 
    {
        return this.x == other.x &&
                this.y == other.y &&
                this.z == other.z &&
                this.w == other.w;                
    }
    
    public void x(double value) {
        this.x = value;
    }
    
    public void r(double value) {
        this.x = value;
    }    
    
    public void y(double value) {
        this.y = value;        
    }
    
    public void g(double value) {
        this.y = value;
    }    
    
    public void z(double value) {
        this.z = value;
    }
    
    public void b(double value) {
        this.z = value;        
    }    
    
    public void w(double value) {
        this.w = value;        
    }
    
    public void a(double value) {
        this.w = value;
    }    
    
    public double x() {
        return x;
    }
    
    public double r() {
        return x;
    }    
    
    public double y() {
        return y;
    }
    
    public double g() {
        return y;
    }    
    
    public double z() {
        return z;
    }
    
    public double b() {
        return z;
    }    
    
    public double w() {
        return w;
    }
    
    public double a() {
        return w;
    }    
    
    public Vector4 minus(Vector4 other) 
    {
        return new Vector4( this.x - other.x , this.y - other.y , this.z - other.z , this.w );
    }
    
    public void minusInPlace(Vector4 other) 
    {
        this.x = this.x - other.x;
        this.y = this.y - other.y;
        this.z = this.z - other.z;
    }    
    
    public double distanceTo(Vector4 point) 
    {
    	double x = this.x - point.x;
    	double y = this.y - point.y;
    	double z = this.z - point.z;
    	return Math.sqrt( x*x + y*y + z*z );
    }
    
    public double squaredDistanceTo(Vector4 point) 
    {
    	double x = this.x - point.x;
    	double y = this.y - point.y;
    	double z = this.z - point.z;
    	return x*x + y*y + z*z;
    }    
    
    public Vector4 plus(Vector4 other) {
        return new Vector4( this.x + other.x , this.y + other.y , this.z + other.z , w );
    }     
    
    public Vector4 plus(Vector4 v1,Vector4 v2) {
        return new Vector4( this.x + v1.x + v2.x , this.y + v1.y +v2.y  , this.z + v1.z + v2.z , w );
    }      
    
    public void plusInPlace(Vector4 v1,Vector4 v2) 
    {
    	this.x = this.x + v1.x + v2.x ;
    	this.y = this.y + v1.y + v2.y ;
    	this.z = this.z + v1.z + v2.z ;
    }        
    
    public void plusInPlace(Vector4 other) 
    {
        this.x = this.x + other.x;
        this.y = this.y + other.y;
        this.z = this.z + other.z;        
    }     
    
    public Vector4(double x,double y,double z) {
        this(x,y,z,1);
    }
    
    public void setToZero() {
    	this.x = this.y = this.z = 0;
    }
    
    public Vector4(double x,double y,double z,double w) 
    {
        this.x = x;
        this.y = y;
        this.z=z;
        this.w=w;
    }
    
    public static Vector4 min(Vector4... vectors) 
    {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        
        for ( Vector4 v : vectors ) 
        {
            
            minX = Math.min( minX , v.x );
            minY = Math.min( minY , v.y );
            minZ = Math.min( minZ , v.z );
        }
        return new Vector4(minX,minY,minZ); 
    }
    
    public static Vector4 max(Vector4... vectors) {
        
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        
        for ( Vector4 v : vectors ) 
        {
            maxX = Math.max(maxX,v.x);
            maxY = Math.max(maxY,v.y);
            maxZ = Math.max(maxZ,v.z);
        }
        return new Vector4(maxX,maxY,maxZ); 
    }    
    
    public Vector4 multiply( Matrix matrix) 
    {
        final double[] matrixData = matrix.data;

        double nx = x * matrixData[0] + y * matrixData[1]+
                    z * matrixData[2]+ w * matrixData[3];
        
        double ny = x * matrixData[4] + y * matrixData[5] +
                    z * matrixData[6] + w * matrixData[7];
        
        double nz = x * matrixData[8] + y * matrixData[9] +
                    z * matrixData[10] + w * matrixData[11];
        
        double nw = x * matrixData[12] + y * matrixData[13] +
                    z * matrixData[14] + w * matrixData[15];
        
        return new Vector4( nx,ny,nz,nw);
    }
    
    public Vector4 multiply(double value) 
    {
        return new Vector4( x * value , y * value , z * value , w );
    }
    
    public Vector4 multiplyAdd(double toMultiply ,Vector4 toAdd) 
    {
        return new Vector4( x * toMultiply + toAdd.x , y * toMultiply + toAdd.y , z * toMultiply + toAdd.z , w );
    }   
    
    public void multiplyAddInPlace(double toMultiply ,Vector4 toAdd) 
    {
    	this.x =  x * toMultiply + toAdd.x;
    	this.y =  y * toMultiply + toAdd.y;
    	this.z =  z * toMultiply + toAdd.z;
    }       
    
    public Vector4 multiplyAdd(Vector4 toMultiply ,Vector4 toAdd) 
    {
        return new Vector4( x * toMultiply.x + toAdd.x , y * toMultiply.y + toAdd.y , z * toMultiply.z + toAdd.z , w );
    }     
    
    public void multiplyInPlace(double value) 
    {
        this.x = this.x * value;
        this.y = this.y * value;
        this.z = this.z * value;
    }    
    
    public Vector4 normalize() 
    {
        final double len = Math.sqrt( x*x + y*y +z*z );
        if ( len  == 0 ) {
        	return new Vector4(); 
        }
        return new Vector4( x / len , y / len , z / len  , w );
    }
    
    public void normalizeInPlace() 
    {
        final double len = Math.sqrt( x*x + y*y +z*z );
        if ( len  != 0 && len != 1 ) 
        {
        	this.x = this.x / len;
        	this.y = this.y / len;
        	this.z = this.z / len;
        }
    }    
    
    public Vector4 normalizeW() 
    {
        if ( w != 1.0 ) 
        {
            return new Vector4( x / w, y / w , z / w , 1 );
        }
        return this;
    }    
    
    public void normalizeWInPlace() 
    {
        if ( w != 1.0 ) 
        {
        	x = x / w ;
        	y = y / w ;
        	z = z / w ;
        }
    }      
    
    // scalar / dot product
    public double dotProduct(Vector4 o) 
    {
        return x*o.x + y*o.y + z * o.z;
    }
    
    public Vector4 straightMultiply(Vector4 o) 
    {
        return new Vector4( this.x * o.x , this.y * o.y , this.z * o.z, this.w * o.w );
    }    
    
    public double length() 
    {
        return Math.sqrt( x*x + y*y +z*z );   
    }
    
    public double magnitude() {
        return x*x + y * y + z * z;   
    }    
    
    public double angleInRadians(Vector4 o) {
        // => cos
        final double cosine = dotProduct( o ) / ( length() * o.length() );
        return Math.acos( cosine );
    }
    
    public double angleInDegrees(Vector4 o) {
        final double factor = (180.0d / Math.PI);
        return angleInRadians(o)*factor;
    }        
    
    public Vector4 crossProduct(Vector4 o) 
    {
        double newX = y * o.z - o.y * z;
        double newY = z * o.x - o.z * x;
        double newZ = x * o.y - o.x * y;
        return new Vector4( newX ,newY,newZ );
    }
    
    @Override
    public String toString()
    {
        return "("+format( x() ) +","+format( y() ) +","+format( z() )+","+format( w() )+")";
    }
    
    private static String format(double d) {
        return new DecimalFormat("##0.0###").format( d );
    }

	public Vector4 clamp(double min, double max) 
	{
		double newX = x;
		double newY = y;
		double newZ = z;
		if ( newX < min ) {
			newX = min;
		} else if ( newX > max ) {
			newX = max;
		}
		if ( newY < min ) {
			newY = min;
		} else if ( newY > max ) {
			newY = max;
		}
		if ( newZ < min ) {
			newZ = min;
		} else if ( newZ > max ) {
			newZ = max;
		}		
		return new Vector4(newX,newY,newZ);
	}
	
	public void clampMagnitudeInPlace(double magnitude) 
	{
		final double len = length();
		if ( len <= magnitude ) {
			return;
		}
		
		final double factor = magnitude / len;
		this.x *= factor;
		this.y *= factor;
		this.z *= factor;
	}
}