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
package de.codesourcery.springmass.springmass;

import java.awt.Color;

import de.codesourcery.springmass.math.Vector4;

public final class SimulationParameters 
{
	private final int xResolution;
	private final int yResolution;

	private final float desiredFPS;
	private final boolean waitForVSync;

	private final boolean renderAllSprings;

	private final boolean renderSprings;

	private final double particleMass;
	
	private final boolean renderMasses;
	
    private final int iterationCount;	
	
	private final double springDampening;
	private final double springCoefficient;
	private final double maxSpringLength;

	private final double mouseDragZDepth;

	private final double verticalRestLengthFactor;
	private final double horizontalRestLengthFactor;
	
	private final boolean debugPerformance;

	private final boolean lightSurfaces;
	private final Vector4 lightPosition;
	private final Vector4 lightColor;

	private final WindParameters windParameters = new WindParameters();
	private final double gravity;
	
	private final int gridColumnCount;
	private final int gridRowCount;
	
	private final double maxParticleSpeed;
	private final int forkJoinBatchSize;
	
	private final double integratonTimeStep;

	public SimulationParameters(int xResolution, 
			int yResolution,
			float desiredFPS, 
			boolean renderAllLines, 
			boolean renderSprings,
			boolean renderMasses, 
			double mouseDragZDepth,
			double verticalRestLengthFactor, 
			double horizontalRestLengthFactor,
			boolean lightSurfaces, 
			Vector4 lightPosition, 
			Color lightColor,
			double gravity, 
			
			int gridColumnCount, 
			int gridRowCount,
			double maxParticleSpeed, 
			int forkJoinBatchSize,
			double springCoefficient,
			double springDampening,
			double particleMass,
			boolean debugPerformance,
			double integratonTimeStep,
			double maxSpringLength,
			int iterationCount,
			boolean waitForVSync,
			WindParameters windParameters) 
	{
		this.xResolution = xResolution;
		this.yResolution = yResolution;
		this.debugPerformance = debugPerformance;
		this.desiredFPS = desiredFPS;
		this.renderAllSprings = renderAllLines;
		this.renderSprings = renderSprings;
		this.renderMasses = renderMasses;
		this.mouseDragZDepth = mouseDragZDepth;
		this.verticalRestLengthFactor = verticalRestLengthFactor;
		this.horizontalRestLengthFactor = horizontalRestLengthFactor;
		this.lightSurfaces = lightSurfaces;
		this.lightPosition = lightPosition;
		this.lightColor = Vector4.valueOf( lightColor );
		this.gravity = gravity;
		this.gridColumnCount = gridColumnCount;
		this.gridRowCount = gridRowCount;
		this.maxParticleSpeed = maxParticleSpeed;
		this.forkJoinBatchSize = forkJoinBatchSize;
		this.springCoefficient = springCoefficient;
		this.maxSpringLength = maxSpringLength;
		this.springDampening = springDampening;
		this.particleMass = particleMass;
		this.integratonTimeStep = integratonTimeStep;
		this.iterationCount = iterationCount;
		this.waitForVSync = waitForVSync;
		this.windParameters.set( windParameters );
	}
	
	public boolean isWaitForVSync()
    {
        return waitForVSync;
    }
	
	public double getIntegrationTimeStep() {
		return integratonTimeStep;
	}
	
	public double getParticleMass() {
		return particleMass;
	}

	public double getGravity() {
		return gravity;
	}
	
	public int getXResolution() {
		return xResolution;
	}

	public int getYResolution() {
		return yResolution;
	}

	public float getDesiredFPS() {
		return desiredFPS;
	}

	public boolean isRenderAllSprings() {
		return renderAllSprings;
	}

	public boolean isRenderSprings() {
		return renderSprings;
	}

	public boolean isRenderMasses() {
		return renderMasses;
	}

	public double getMouseDragZDepth() {
		return mouseDragZDepth;
	}

	public double getVerticalRestLengthFactor() {
		return verticalRestLengthFactor;
	}

	public double getHorizontalRestLengthFactor() {
		return horizontalRestLengthFactor;
	}

	public boolean isLightSurfaces() {
		return lightSurfaces;
	}

	public Vector4 getLightPosition() {
		return lightPosition;
	}

	public Vector4 getLightColor() {
		return lightColor;
	}

	public int getGridColumnCount() {
		return gridColumnCount;
	}

	public int getGridRowCount() {
		return gridRowCount;
	}

	public double getMaxParticleSpeed() {
		return maxParticleSpeed;
	}

	public int getForkJoinBatchSize() {
		return forkJoinBatchSize;
	}

	public double getSpringDampening() {
		return springDampening;
	}

	public double getSpringCoefficient() {
		return springCoefficient;
	}

	public boolean isDebugPerformance() {
		return debugPerformance;
	}

	public double getMaxSpringLength() {
		return maxSpringLength;
	}
	
	public int getIterationCount()
    {
        return iterationCount;
    }
	
	public WindParameters getWindParameters() {
		return windParameters;
	}
}
