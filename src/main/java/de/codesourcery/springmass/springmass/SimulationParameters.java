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

import com.badlogic.gdx.math.Vector3;

import de.codesourcery.springmass.math.VectorUtils;

public final class SimulationParameters 
{
	private final int xResolution;
	private final int yResolution;

	private final float desiredFPS;
	private final boolean waitForVSync;

	private final boolean renderAllSprings;

	private final boolean renderSprings;

	private final float particleMass;
	
	private final boolean renderMasses;
	
    private final int iterationCount;	
	
	private final float springDampening;
	private final float springCoefficient;
	private final float maxSpringLength;

	private final float mouseDragZDepth;

	private final float verticalRestLengthFactor;
	private final float horizontalRestLengthFactor;
	
	private final boolean debugPerformance;

	private final boolean lightSurfaces;
	private final Vector3 lightPosition;
	private final Vector3 lightColor;

	private final WindParameters windParameters = new WindParameters();
	private final float gravity;
	
	private final int gridColumnCount;
	private final int gridRowCount;
	
	private final float maxParticleSpeed;
	private final int forkJoinBatchSize;
	
	private final float integratonTimeStep;

	public SimulationParameters(int xResolution, 
			int yResolution,
			float desiredFPS, 
			boolean renderAllLines, 
			boolean renderSprings,
			boolean renderMasses, 
			float mouseDragZDepth,
			float verticalRestLengthFactor, 
			float horizontalRestLengthFactor,
			boolean lightSurfaces, 
			Vector3 lightPosition, 
			Color lightColor,
			float gravity, 
			
			int gridColumnCount, 
			int gridRowCount,
			float maxParticleSpeed, 
			int forkJoinBatchSize,
			float springCoefficient,
			float springDampening,
			float particleMass,
			boolean debugPerformance,
			float integratonTimeStep,
			float maxSpringLength,
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
		this.lightColor = VectorUtils.valueOf( lightColor );
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
	
	public float getIntegrationTimeStep() {
		return integratonTimeStep;
	}
	
	public float getParticleMass() {
		return particleMass;
	}

	public float getGravity() {
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

	public float getMouseDragZDepth() {
		return mouseDragZDepth;
	}

	public float getVerticalRestLengthFactor() {
		return verticalRestLengthFactor;
	}

	public float getHorizontalRestLengthFactor() {
		return horizontalRestLengthFactor;
	}

	public boolean isLightSurfaces() {
		return lightSurfaces;
	}

	public Vector3 getLightPosition() {
		return lightPosition;
	}

	public Vector3 getLightColor() {
		return lightColor;
	}

	public int getGridColumnCount() {
		return gridColumnCount;
	}

	public int getGridRowCount() {
		return gridRowCount;
	}

	public float getMaxParticleSpeed() {
		return maxParticleSpeed;
	}

	public int getForkJoinBatchSize() {
		return forkJoinBatchSize;
	}

	public float getSpringDampening() {
		return springDampening;
	}

	public float getSpringCoefficient() {
		return springCoefficient;
	}

	public boolean isDebugPerformance() {
		return debugPerformance;
	}

	public float getMaxSpringLength() {
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
