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

	private final int frameSleepTime;

	private final boolean renderAllLines;

	private final boolean renderSprings;

	private final double particleMass;
	
	private final boolean renderMasses;
	
	private final double springDampening;
	private final double springCoefficient;

	private final double mouseDragZDepth;

	private final double verticalRestLengthFactor;
	private final double horizontalRestLengthFactor;

	private final boolean lightSurfaces;
	private final Vector4 lightPosition;
	private final Vector4 lightColor;

	private final double gravity;
	
	private final int gridColumnCount;
	private final int gridRowCount;
	
	private final double maxParticleSpeed;
	private final int forkJoinBatchSize;

	public SimulationParameters() 
	{
		xResolution = 1000;
		yResolution = 1000;

		frameSleepTime = 20;

		renderAllLines = false;

		renderSprings = false;

		renderMasses = false;

		mouseDragZDepth = -100;

		particleMass = 1.0;
		
		springDampening = springCoefficient = 0.1;
		
		verticalRestLengthFactor = 1;
		horizontalRestLengthFactor = 1;

		lightSurfaces = true;
		lightPosition = new Vector4(getXResolution() / 3.5, getYResolution() / 2.5, -200);
		lightColor = new Vector4(.2, .2, 0.8);

		gravity=9.81;
		
		gridColumnCount = 33;
		gridRowCount = 33;
		
		maxParticleSpeed = 20;
		forkJoinBatchSize = 1000;		
	}
	
	public SimulationParameters(int xResolution, int yResolution,
			int frameSleepTime, boolean renderAllLines, boolean renderSprings,
			boolean renderMasses, double mouseDragZDepth,
			double verticalRestLengthFactor, double horizontalRestLengthFactor,
			boolean lightSurfaces, Vector4 lightPosition, Color lightColor,
			double gravity, int gridColumnCount, int gridRowCount,
			double maxParticleSpeed, int forkJoinBatchSize,
			double springCoefficient,double springDampening,double particleMass) 
	{
		this.xResolution = xResolution;
		this.yResolution = yResolution;
		this.frameSleepTime = frameSleepTime;
		this.renderAllLines = renderAllLines;
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
		this.springDampening = springDampening;
		this.particleMass = particleMass;
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

	public int getFrameSleepTime() {
		return frameSleepTime;
	}

	public boolean isRenderAllLines() {
		return renderAllLines;
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
}
