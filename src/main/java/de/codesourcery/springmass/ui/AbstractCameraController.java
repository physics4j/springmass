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
package de.codesourcery.springmass.ui;


public abstract class AbstractCameraController implements ICameraController 
{
	@Override
	public void translate(float dx, float dy, float dz) {
		setPosition( getPosition().add( dx,dy,dz ) );
	}

	@Override
	public void rotate(float angleX, float angleY) 
	{
		if ( angleX != 0 ) 
		{
			/*
			 * fX = angleY / angleX;
			 * 
			 * => angleY = fX * angleX;
			 */
			float fX = angleY / angleX; 
			setViewDirection( this.getViewDirection().rotate(angleX , 1 , fX * angleX , 0 ) );
		} 
		else if ( angleY != 0 ) 
		{
			/*
			 * fX = angleX / angleY;
			 * 
			 * => angleX = fX * angleY;
			 */					
			float fX = angleX / angleY; 
			this.setViewDirection( getViewDirection().rotate(angleY , fX * angleY , 1 , 0 ) );					
		}
	}

	@Override
	public void viewportChanged(int width, int height) {
		
	}

	@Override
	public void cameraChanged() {
	}
}
