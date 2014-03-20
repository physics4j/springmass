package de.codesourcery.springmass.render;


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
