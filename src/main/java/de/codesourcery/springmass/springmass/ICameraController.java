package de.codesourcery.springmass.springmass;

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
