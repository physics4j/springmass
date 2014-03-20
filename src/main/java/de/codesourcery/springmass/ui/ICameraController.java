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

import com.badlogic.gdx.math.Vector3;

public interface ICameraController {

	public Vector3 getPosition();
	
	public void setPosition(Vector3 position);
	
	public Vector3 getViewDirection();
	
	public void setViewDirection(Vector3 dir);
	
	public void translate(float dx,float dy,float dz);
	
	public void rotate(float angleX,float angleY);
	
	public void viewportChanged(int width,int height);
	
	public void cameraChanged();
}
