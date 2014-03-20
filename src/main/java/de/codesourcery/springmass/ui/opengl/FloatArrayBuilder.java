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
package de.codesourcery.springmass.ui.opengl;

import com.badlogic.gdx.math.Vector3;

public final class FloatArrayBuilder {

	public float[] array;
	private int currentOffset=0;
	private final int sizeIncrement;
	
	public FloatArrayBuilder(int initialSize,int sizeIncrement) 
	{
		if ( initialSize < 1 ) {
			throw new IllegalArgumentException("initial size must be >= 1");
		}
		if ( sizeIncrement < 1 ) {
			throw new IllegalArgumentException("size increment must be >= 1");
		}
		this.array = new float[ initialSize ];
		this.sizeIncrement = sizeIncrement;
	}
	
	public void put(float value) 
	{
		if ( currentOffset == array.length-1) {
			extendArray(1);
		}
		array[currentOffset++]=value;
	}
	
	public void put(float[] data,int offset,int count) 
	{
		if ( currentOffset+count >= array.length-1) {
			extendArray(count);
		}
		System.arraycopy( data , offset , array , currentOffset , count );
		currentOffset += count;
	}	
	
	public FloatArrayBuilder begin() 
	{
		currentOffset = 0;
		return this;
	}
	
	public void put(Vector3 v) {
		put( v.x , v.y , v.z );
	}
	
	public void put(Vector3 v,float w) {
		put( v.x , v.y , v.z , w );
	}		
	
	public int end() 
	{
		return currentOffset;
	}	
	
	public int actualSize() {
		return currentOffset;
	}
	
	public void put(float value1,float value2,float value3) 
	{
		if ( currentOffset+3 >= array.length-1) {
			extendArray(3);
		}
		array[currentOffset++]=value1;
		array[currentOffset++]=value2;
		array[currentOffset++]=value3;
	}	
	
	public void put(float[] data) 
	{
		final int length = data.length;
		if ( currentOffset+length>= array.length-1) {
			extendArray(length);
		}
		System.arraycopy( data , 0 , array , currentOffset , length);
		currentOffset += length;
	}		
	
	public void put(float value1,float value2,float value3,float value4) 
	{
		if ( currentOffset+4 >= array.length-1) {
			extendArray(4);
		}
		array[currentOffset++]=value1;
		array[currentOffset++]=value2;
		array[currentOffset++]=value3;
		array[currentOffset++]=value4;
	}	
	
	public void put(float value1,float value2,float value3,float value4,float value5,float value6,float value7) 
	{
		if ( currentOffset+7 >= array.length-1) {
			extendArray(7);
		}
		array[currentOffset++]=value1;
		array[currentOffset++]=value2;
		array[currentOffset++]=value3;
		array[currentOffset++]=value4;
		array[currentOffset++]=value5;
		array[currentOffset++]=value6;
		array[currentOffset++]=value7;
	}		
	
	public void put(float value1,float value2,float value3,float value4,float value5,float value6,float value7,float value8) 
	{
		if ( currentOffset+8 >= array.length-1) {
			extendArray(8);
		}
		array[currentOffset++]=value1;
		array[currentOffset++]=value2;
		array[currentOffset++]=value3;
		array[currentOffset++]=value4;
		array[currentOffset++]=value5;
		array[currentOffset++]=value6;
		array[currentOffset++]=value7;
		array[currentOffset++]=value8;		
	}	
	
	public void put(float value1,float value2,float value3,float value4,float value5,float value6,float value7,float value8,float value9) 
	{
		if ( currentOffset+9 >= array.length-1) {
			extendArray(9);
		}
		array[currentOffset++]=value1;
		array[currentOffset++]=value2;
		array[currentOffset++]=value3;
		array[currentOffset++]=value4;
		array[currentOffset++]=value5;
		array[currentOffset++]=value6;
		array[currentOffset++]=value7;
		array[currentOffset++]=value8;		
		array[currentOffset++]=value9;				
	}	
	
	public void put(float value1,float value2,float value3,float value4,float value5,float value6,float value7,float value8,float value9,float value10) 
	{
		if ( currentOffset+10 >= array.length-1) {
			extendArray(10);
		}
		array[currentOffset++]=value1;
		array[currentOffset++]=value2;
		array[currentOffset++]=value3;
		array[currentOffset++]=value4;
		array[currentOffset++]=value5;
		array[currentOffset++]=value6;
		array[currentOffset++]=value7;
		array[currentOffset++]=value8;
		array[currentOffset++]=value9;
		array[currentOffset++]=value10;
	}	

	private void extendArray(int minIncrement) 
	{
		int newSize = sizeIncrement < minIncrement ? minIncrement : sizeIncrement;
		float[] tmp = new float[ array.length + newSize ];
		System.arraycopy(array, 0 , tmp , 0 , currentOffset);
		array=tmp;
	}
}