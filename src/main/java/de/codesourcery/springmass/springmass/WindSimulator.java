package de.codesourcery.springmass.springmass;

import java.util.Random;

import de.codesourcery.springmass.math.Vector4;

public class WindSimulator {

	private WindParameters params;
	
	private final Vector4 currentDirection=new Vector4(); 	
	private final Vector4 desiredDirection=new Vector4();
	
	private float currentForce;
	private float desiredForce;
	
	private final Vector4 directionIncrement=new Vector4();
	private float forceIncrement=0;
	
	private int stepCount;
	private Random random; 
	
	public WindSimulator(Random random,WindParameters params) {
		this.params = params;
		this.random = random;
		generateNewDirectionAndForce();
	}
	
	public void set(WindSimulator other) 
	{
		this.params = other.params;
		this.currentDirection.set( other.currentDirection );
		this.desiredDirection.set( other.desiredDirection );
		this.currentForce = other.currentForce;
		this.desiredForce = other.desiredForce;
		
		this.directionIncrement.set( other.directionIncrement );
		this.forceIncrement = other.forceIncrement;
		
		this.stepCount = other.stepCount;
		this.random = other.random;
	}
	
	public void step() 
	{
		stepCount++;
		if ( params.isEnabled() ) 
		{
			if ( (stepCount % params.getStepsUntilDirectionChanged()) == 0 ) 
			{
				generateNewDirectionAndForce();
			} else {
				stepDirectionAndForce();
			}
		}
	}
	
	private void stepDirectionAndForce() 
	{
		if ( Math.abs( desiredForce - currentForce ) > 0.01 ) {
			currentForce += forceIncrement;
			// System.out.println( stepCount+" : stepped force to "+currentForce);
		}
		if ( Math.abs( currentDirection.distanceSquaredTo( desiredDirection ) ) > 0.01*0.01 ) {
			currentDirection.plusInPlace( directionIncrement );
			// System.out.println( stepCount+" : stepped  direction to "+currentDirection);
		}
	}

	private void generateNewDirectionAndForce() 
	{
		if ( ! params.isEnabled() ) 
		{
			directionIncrement.set(0,0,0);
			forceIncrement=0;
			desiredForce=currentForce=0;
			currentDirection.set( params.getMinDirection() );
			desiredDirection.set( currentDirection );
			System.out.println("*** ["+stepCount+"] wind: DISABLED");
			return;
		}
		
		// generate new random force
		desiredForce = params.getMinForce()+random.nextFloat()*( params.getMaxForce() - params.getMinForce() );
		forceIncrement = (desiredForce - currentForce)/(float) params.getStepsUntilDirectionAdjusted(); 
		
		System.out.println("*** ["+stepCount+"] wind: current_force="+currentForce+", new_force="+desiredForce+" , increment="+forceIncrement+" ( "+params.getStepsUntilDirectionAdjusted()+" steps)");
		
		// generate new random direction
		desiredDirection.set( params.getMaxDirection() );
		desiredDirection.minusInPlace( params.getMinDirection() );
		double len = desiredDirection.length();
		
		double factor = random.nextFloat()*len;
		desiredDirection.multiplyAddInPlace( factor , params.getMinDirection() );
		
		directionIncrement.set( desiredDirection );
		directionIncrement.minusInPlace( currentDirection );
		directionIncrement.multiplyInPlace( 1.0/ params.getStepsUntilDirectionAdjusted() );
		
		System.out.println("*** ["+stepCount+"] wind: current_direction="+currentDirection+", new_direction="+desiredDirection+" , increment="+directionIncrement+" ( "+params.getStepsUntilDirectionAdjusted()+" steps)");
				
	}
	
	public void getCurrentWindVector(Vector4 v) {
		v.set( currentDirection );
		v.normalizeInPlace();
		v.multiplyInPlace( currentForce );
	}	
}