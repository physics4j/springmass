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

public final class ShortArrayBuilder {

	public short[] array;
	private int currentOffset=0;
	private int sizeIncrement;
	
	public ShortArrayBuilder(int initialSize,int sizeIncrement) 
	{
		if ( initialSize < 1 ) {
			throw new IllegalArgumentException("initial size must be >= 1");
		}
		if ( sizeIncrement < 1 ) {
			throw new IllegalArgumentException("size increment must be >= 1");
		}
		
		this.array = new short[ initialSize ];
		this.sizeIncrement = sizeIncrement;
	}
	
	public ShortArrayBuilder put(short value) 
	{
		if ( currentOffset == array.length-1) {
			extendArray(1);
		}
		array[currentOffset++]=value;
		return this;
	}
	
	public ShortArrayBuilder put(short[] data) 
	{
		final int length = data.length;
		if ( currentOffset+length>= array.length-1) {
			extendArray(length);
		}
		System.arraycopy( data , 0 , array , currentOffset , length);
		currentOffset += length;
		return this;
	}	
	
	public ShortArrayBuilder put(short[] data,int offset,int count) 
	{
		if ( currentOffset+count >= array.length-1) {
			extendArray(count);
		}
		System.arraycopy( data , offset , array , currentOffset , count );
		currentOffset += count;
		return this;
	}		
	
	public ShortArrayBuilder begin() 
	{
		currentOffset = 0;
		return this;
	}
	
	public int end() 
	{
		return currentOffset;
	}	
	
	public int actualSize() {
		return currentOffset;
	}
	
	public ShortArrayBuilder put(short value1,short value2,short value3) 
	{
		if ( currentOffset+3 >= array.length-1) {
			extendArray(3);
		}
		array[currentOffset++]=value1;
		array[currentOffset++]=value2;
		array[currentOffset++]=value3;
		return this;
	}	
	
	public ShortArrayBuilder put(short value1,short value2,short value3,short value4) 
	{
		if ( currentOffset+4 >= array.length-1) {
			extendArray(4);
		}
		array[currentOffset++]=value1;
		array[currentOffset++]=value2;
		array[currentOffset++]=value3;
		array[currentOffset++]=value4;
		return this;
	}	
	
	public ShortArrayBuilder put(short value1,short value2,short value3,short value4,short value5 , short value6) 
	{
		if ( currentOffset+6 >= array.length-1) {
			extendArray(6);
		}
		array[currentOffset++]=value1;
		array[currentOffset++]=value2;
		array[currentOffset++]=value3;
		array[currentOffset++]=value4;
		array[currentOffset++]=value5;
		array[currentOffset++]=value6;
		return this;
	}	

	private void extendArray(int minIncrement) 
	{
		int newSize = sizeIncrement < minIncrement ? minIncrement : sizeIncrement;
		short[] tmp = new short[ array.length + newSize ];
		System.arraycopy(array, 0 , tmp , 0 , currentOffset);
		array=tmp;
	}
}