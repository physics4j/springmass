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
