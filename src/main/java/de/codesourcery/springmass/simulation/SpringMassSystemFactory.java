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
package de.codesourcery.springmass.simulation;

import java.awt.Color;
import java.util.Random;

import com.badlogic.gdx.math.Vector3;

public class SpringMassSystemFactory {

	private Spring createSpring(Mass m1,Mass m2,float restLength , boolean doRender,Color color,SimulationParameters parameters) 
	{
		return new Spring(m1, m2, restLength, doRender, color, parameters.getSpringCoefficient() );
	}
	
	private Spring createSpring(Mass m1,Mass m2,float restLength , boolean doRender,SimulationParameters parameters) 
	{
		return createSpring(m1, m2, restLength, doRender, Color.green , parameters);
	}	
	
	public SpringMassSystem create(SimulationParameters parameters) 
	{
		System.out.println("Point masses: "+(parameters.getGridRowCount()*parameters.getGridColumnCount()));
		
		final Mass[][] masses = new Mass[parameters.getGridColumnCount()][];
		for ( int i = 0 ; i < parameters.getGridColumnCount() ; i++ ) {
			masses[i] = new Mass[parameters.getGridRowCount()];
		}
		
		int springCount = 0;

		float scaleX = parameters.getXResolution() / (parameters.getGridColumnCount()+parameters.getGridColumnCount()*0.5f);
		float scaleY = parameters.getYResolution() / (parameters.getGridRowCount()+parameters.getGridRowCount()*0.5f);
		
		final float factorDecrement = 1.0f / parameters.getGridRowCount();
		final float xOffset = parameters.getXResolution()*0.2f;
		final float yOffset = scaleY;

		float minX=Float.MAX_VALUE;
		float maxX=Float.MIN_VALUE;
		
		float minY=Float.MAX_VALUE;
		float maxY=Float.MIN_VALUE;
		
		float minZ=Float.MAX_VALUE;
		float maxZ=Float.MIN_VALUE;
		
		for ( int x = 0 ; x < parameters.getGridColumnCount() ; x++ ) 
		{
			float factor = 1.0f;
			for ( int y = 0 ; y < parameters.getGridRowCount() ; y++ ) 
			{
				final Vector3 pos = new Vector3( xOffset + scaleX*x , yOffset + scaleY*factor*y,-10);
				minX = Math.min( minX , pos.x );
				minY = Math.min( minX , pos.y);
				minZ = Math.min( minX , pos.z );
				maxX = Math.max( maxX , pos.x );
				maxY = Math.max( maxY , pos.y );
				maxZ = Math.max( maxZ , pos.z );
				
				final Mass m = new Mass( Color.red  , pos , parameters.getParticleMass() );
				if ( y == 0 ) {
					m.setFixed( true );
				}
				masses[x][y] = m;
				factor -= factorDecrement;
			}
		}
		
		System.out.println("Min X/Y = "+ new Vector3(minX,minY,minZ));
		System.out.println("Max X/Y = "+ new Vector3(maxX,maxY,maxZ));
		
		final Random random = new Random(0xdeadbeef);
		final SpringMassSystem system = new SpringMassSystem(parameters,masses,random);

		// connect masses horizontally
		final float horizRestLength = scaleX*parameters.getHorizontalRestLengthFactor();
		for ( int y = 0 ; y < parameters.getGridRowCount() ; y++ ) 
		{
			for ( int x = 0 ; x < (parameters.getGridColumnCount()-1) ; x++ ) 
			{
				system.addSpring( createSpring( masses[x][y] , masses[x+1][y] , horizRestLength , true , parameters ) );
				springCount++;
			}
		}

		// connect masses vertically
		final float verticalRestLength = scaleY*parameters.getVerticalRestLengthFactor();
		for ( int x = 0 ; x < parameters.getGridColumnCount() ; x++ ) 
		{
			for ( int y = 0 ; y < (parameters.getGridRowCount()-1) ; y++ ) 
			{
				system.addSpring( createSpring( masses[x][y] , masses[x][y+1] , verticalRestLength , true , parameters ) );
				springCount++;
			}
		}	

		// cross-connect masses
		final float crossConnectRestLength = (float) Math.sqrt( horizRestLength*horizRestLength + verticalRestLength*verticalRestLength);
		for ( int x = 0 ; x < (parameters.getGridColumnCount()-1) ; x++ ) 
		{
			for ( int y = 0 ; y < (parameters.getGridRowCount()-1) ; y++ ) 
			{
				system.addSpring( createSpring( masses[x][y] , masses[x+1][y+1] , crossConnectRestLength , parameters.isRenderAllSprings() , Color.YELLOW , parameters ) );
				system.addSpring( createSpring( masses[x][y+1] , masses[x+1][y] , crossConnectRestLength , parameters.isRenderAllSprings() , Color.YELLOW , parameters ) );
				springCount+=2;
			}
		}	

		// connect cloth outline
		final float horizOutlineRestLength = 2 * horizRestLength;
		for ( int y = 0 ; y < parameters.getGridRowCount() ; y++ ) {
			for ( int x = 0 ; x < (parameters.getGridColumnCount()-2) ; x++ ) 
			{
				system.addSpring( createSpring( masses[x][y] , masses[x+2][y] , horizOutlineRestLength , parameters.isRenderAllSprings(), Color.BLUE , parameters ) );
				springCount++;
			}	
		}

		final float verticalOutlineRestLength = 2 * verticalRestLength;
		for ( int x = 0 ; x < parameters.getGridColumnCount() ; x++ ) 
		{ 
			for ( int y = 0 ; y < (parameters.getGridRowCount()-2) ; y++ ) 
			{
				system.addSpring( createSpring( masses[x][y] , masses[x][y+2] , verticalOutlineRestLength , parameters.isRenderAllSprings(), Color.BLUE , parameters ) );
				springCount++;				
			}		
		}
		System.out.println("Springs: "+springCount);
		return system;
	}
}
