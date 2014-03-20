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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

public abstract class FPSCameraController extends InputAdapter 
{
	private boolean strafePressed = false;
	private boolean strafeLeft=false;
	
	private int lastX=-1;
	private int lastY=-1;
	
	/** The angle to rotate when moved the full width or height of the screen. */
	public float rotateAngle = 360f;
	
	/** The units to translate the camera when moved the full width or height of the screen. */
	public float translateUnits = 400f; 
	
	/** The target to rotate around. */
	public final Vector3 target = new Vector3();
	
	private int mouseLookKey = Keys.M;
	private boolean mayUseMouseLook = false;
	
	public int forwardKey = Keys.W;
	protected boolean forwardPressed;
	
	public int backwardKey = Keys.S;
	protected boolean backwardPressed;
	
	public int upKey = Keys.Q;
	protected boolean upPressed;
	
	public int downKey = Keys.E;
	protected boolean downPressed;	
	
	/** The camera. */
	public Camera camera;
	
	private final Vector3 tmpV1 = new Vector3();
	
	private final Matrix4 TMP_MATRIX = new Matrix4().idt();
	public final Matrix3 normalMatrix = new Matrix3().idt();
	
	public FPSCameraController(final Camera camera,Vector3 initialDirection) 
	{
		this.camera = camera;
		updateNormalMatrix();
	}
	
	private void updateNormalMatrix() {
		TMP_MATRIX.set( camera.view ).inv().tra();
		normalMatrix.set( TMP_MATRIX );
	}
	
	public void update() 
	{
		if ( forwardPressed || backwardPressed || strafePressed || upPressed || downPressed ) 
		{
			final float delta = Gdx.graphics.getDeltaTime();
			
			boolean updateRequired = false;
			if ( strafePressed ) 
			{
				float deltaX = strafeLeft ? -2f : 2f;
				updateRequired |= translateCamera(tmpV1.set(camera.direction).crs(camera.up).nor().scl(deltaX *delta* translateUnits));
			}
			
			if (forwardPressed) {
				updateRequired |= translateCamera(tmpV1.set(camera.direction).scl(delta * translateUnits));
			} 
			else if (backwardPressed) 
			{
				updateRequired |= translateCamera(tmpV1.set(camera.direction).scl(-delta * translateUnits));
			}
			if ( upPressed ) {
				updateRequired |= translateCamera(tmpV1.set(0,1,0).scl( delta * translateUnits ) );
			} else if ( downPressed ) {
				updateRequired |= translateCamera(tmpV1.set(0,1,0).scl( -delta * translateUnits ) );				
			}
			
			if ( updateRequired ) 
			{
				updateCamera();
				cameraTranslated(this.camera);
			}
		}
	}
	
	private void updateCamera() 
	{
		camera.update();
		updateNormalMatrix();
	}
	
	public boolean translateCamera(Vector3 v) 
	{
		if ( canTranslateCamera( this.camera , v ) ) {
			camera.translate(v);
			target.add(v);
			return true;
		}
		return false;
	}
	
	public abstract boolean canTranslateCamera(Camera cam,Vector3 posDelta); 
	
	@Override
	public boolean keyDown (int keycode) 
	{
		if ( isStrafeKey(keycode ) ) 
		{
			strafePressed = true;
			strafeLeft = isStrafeLeftKey( keycode );
			return false;
		}
		
		if ( keycode == upKey ) {
			upPressed = true;
		} else if ( keycode == downKey ) {
			downPressed = true;
		}
		
		if (keycode == forwardKey) { 
			forwardPressed = true;
		} else if (keycode == backwardKey) {
			backwardPressed = true;
		}
		return false;
	}
	
	@Override
	public boolean keyUp (int keycode) 
	{
		if ( keycode == Input.Keys.ESCAPE) {
			Gdx.input.setCursorCatched(false);
			return false;
		}
		
		if ( keycode == mouseLookKey ) 
		{
			mayUseMouseLook = ! mayUseMouseLook;
			System.out.println("Mouse look "+( mayUseMouseLook ? "enabled" : "disabled"));
		} 
		else if ( isStrafeKey(keycode ) ) 
		{
			strafePressed = false;
		} 
		if (keycode == forwardKey) 
		{
			forwardPressed = false;
		}
		else if (keycode == backwardKey) 
		{
			backwardPressed = false;
		} 
		if ( keycode == upKey ) {
			upPressed = false;
		} else if ( keycode == downKey ) {
			downPressed = false;
		}		
		return false;
	}
	
	@Override
	public boolean mouseMoved (int screenX, int screenY) 
	{
		if ( ! Gdx.input.isCursorCatched() ) {
			lastX = lastY = -1;
			return false;
		}
		
		if ( lastX == -1 || lastY == -1 ) {
			lastX = screenX;
			lastY = screenY;
			return false;
		}
		
		final float deltaX = (screenX - lastX) / (float) Gdx.graphics.getWidth();
		final float deltaY = (lastY - screenY) / (float) Gdx.graphics.getHeight();
		
		tmpV1.set(camera.direction).crs(camera.up).y = 0f;
		
		// rotation around X axis
		final float rotXAxis = deltaY * rotateAngle;
		camera.rotateAround(camera.position, tmpV1.nor(), rotXAxis);
		
		// rotation around Y axis
		float rotYAxis = deltaX * -rotateAngle;
		camera.rotateAround(camera.position, Vector3.Y, rotYAxis);
		
		lastX = screenX;
		lastY = screenY;

		updateCamera();
		cameraRotated(this.camera);		
		return false;
	}
	
	@Override
	public boolean touchDown (int screenX, int screenY, int pointer, int button) 
	{
		if ( mayUseMouseLook && ! Gdx.input.isCursorCatched() ) {
			Gdx.input.setCursorCatched(true);
			return false;
		}
		
		if ( button == Input.Buttons.LEFT ) 
		{
			onLeftClick(screenX,screenY);
		} 
		else if ( button == Input.Buttons.RIGHT ) {
			onRightClick(screenX,screenY);
		}
		return false;
	}
	
	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		// TODO Auto-generated method stub
		return super.touchUp(screenX, screenY, pointer, button);
	}
	
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		return super.touchDragged(screenX, screenY, pointer);
	};
	
	public abstract void cameraTranslated(Camera camera);
	
	public abstract void cameraRotated(Camera camera);
	
	public abstract void onLeftClick(int screenX,int screenY);
	
	public abstract void onRightClick(int screenX,int screenY);	
	
	private boolean isStrafeLeftKey(int keyCode) { return keyCode == Input.Keys.A; }
	
	private boolean isStrafeRightKey(int keyCode) { return keyCode == Input.Keys.D; }		
	
	private boolean isStrafeKey(int keyCode) 
	{
		return isStrafeLeftKey(keyCode) || isStrafeRightKey(keyCode);
	}	
}