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

import com.badlogic.gdx.math.Vector3;

public class VectorUtils {

    public static Vector3 valueOf(Color c) 
    {
    	return new Vector3( c.getRed() / 255.0f , c.getGreen() / 255.0f , c.getBlue() / 255.0f );
    }
    
    public static int toRGB(Vector3 v) 
    {
    	int r = (int) Math.max( 0 , Math.min(v.x * 255f,255) );
    	int g = (int) Math.max( 0 , Math.min(v.y * 255f,255) );
    	int b = (int) Math.max( 0 , Math.min(v.z * 255f,255) );
        int color = r << 16 | g << 8 | b;
        return color;
    }
    
	public static void clampMagnitudeInPlace(Vector3 v , double magnitude) 
	{
		final double len = v.len();
		if ( len <= magnitude ) {
			return;
		}
		
		final double factor = magnitude / len;
		v.x *= factor;
		v.y *= factor;
		v.z *= factor;
	}    
    
    public static Color toColor(Vector3 v) {
    	return new Color( toRGB(v) );
    }
}
