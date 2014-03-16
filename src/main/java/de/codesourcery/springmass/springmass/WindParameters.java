package de.codesourcery.springmass.springmass;


public final class WindParameters {

	private final SphericalCoordinates minAngle=new SphericalCoordinates();
	private final SphericalCoordinates maxAngle=new SphericalCoordinates();
	
	private float minForce;
	private float maxForce;
	
	private int stepsUntilDirectionAdjusted=10;
	private int stepsUntilDirectionChanged=10;
	
	private boolean enabled;

	public WindParameters() {
	}
	
	public WindParameters(WindParameters other) {
		set(other);
	}
	
	public void set(WindParameters other) 
	{
		this.minAngle.set( other.minAngle );
		this.maxAngle.set( other.maxAngle );
		
		this.minForce = other.minForce;
		this.maxForce = other.maxForce;
		
		this.stepsUntilDirectionAdjusted = other.stepsUntilDirectionAdjusted;
		this.stepsUntilDirectionChanged = other.stepsUntilDirectionAdjusted;
		
		this.enabled = other.enabled;
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getStepsUntilDirectionAdjusted() {
		return stepsUntilDirectionAdjusted;
	}
	
	public int getStepsUntilDirectionChanged() {
		return stepsUntilDirectionChanged;
	}
	
	public void setStepsUntilDirectionAdjusted(int stepsUntilDirectionAdjusted) {
		if ( stepsUntilDirectionAdjusted < 1 ) {
			throw new IllegalArgumentException("Value must be >= 1");
		}
		this.stepsUntilDirectionAdjusted = stepsUntilDirectionAdjusted;
	}
	
	public void setStepsUntilDirectionChanged(int stepsUntilDirectionChanged) {
		if ( stepsUntilDirectionChanged < 1 ) {
			throw new IllegalArgumentException("Value must be >= 1");
		}		
		this.stepsUntilDirectionChanged = stepsUntilDirectionChanged;
	}
	
	public float getMinForce() {
		return minForce;
	}

	public void setMinForce(float minForce) {
		this.minForce = minForce;
	}

	public float getMaxForce() {
		return maxForce;
	}

	public void setMaxForce(float maxForce) {
		this.maxForce = maxForce;
	}

	public void setMinAngle(SphericalCoordinates coords) {
		this.minAngle.set( coords );
	}
	
	public void setMaxAngle(SphericalCoordinates coords) {
		this.maxAngle.set( coords );
	}
	
	public SphericalCoordinates getMinAngle() {
		return minAngle;
	}
	
	public SphericalCoordinates getMaxAngle() {
		return maxAngle;
	}
}