package de.codesourcery.springmass.springmass;

import de.codesourcery.springmass.math.Vector4;

public final class WindParameters {

	private final Vector4 minDirection=new Vector4(); // normalized vector
	private final Vector4 maxDirection=new Vector4(); // normalized vector
	
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
		this.minDirection.set( other.minDirection );
		this.maxDirection.set( other.maxDirection );
		
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

	public Vector4 getMinDirection() {
		return minDirection;
	}
	
	public void setMinDirection(Vector4  dir) {
		minDirection.set(dir);
		minDirection.normalizeInPlace();
	}

	public Vector4 getMaxDirection() {
		return maxDirection;
	}
	
	public void setMaxDirection(Vector4 dir) {
		maxDirection.set(dir);
		maxDirection.normalizeInPlace();
	}	
}