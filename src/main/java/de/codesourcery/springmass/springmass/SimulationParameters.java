package de.codesourcery.springmass.springmass;

import de.codesourcery.springmass.math.Vector4;

public final class SimulationParameters 
{
	private final int xResolution = 1000;
	private final int yResolution = 1000;

	private final int frameSleepTime = 20;

	private final boolean renderAllLines = false;

	private final boolean renderSprings = false;

	private final boolean renderMasses = false;

	private final double mouseDragZDepth = -100;

	private final double verticalRestLengthFactor = 1;
	private final double horizontalRestLengthFactor = 1;

	private final boolean lightSurfaces = true;
	private final Vector4 lightPosition = new Vector4(getXResolution() / 3.5, getYResolution() / 2.5, -200);
	private final Vector4 lightColor = new Vector4(.2, .2, 0.8);

	private final int gridColumns = 633;
	private final int gridRows = 33;
	
	private final double maxParticleSpeed = 20;
	private final int forkJoinBatchSize = 100;

	public SimulationParameters() {
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

	public int getGridColumns() {
		return gridColumns;
	}

	public int getGridRows() {
		return gridRows;
	}

	public double getMaxParticleSpeed() {
		return maxParticleSpeed;
	}

	public int getForkJoinBatchSize() {
		return forkJoinBatchSize;
	}
}
