package de.codesourcery.springmass.springmass;

import de.codesourcery.springmass.math.Vector4;

public class SimulationParameters {
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
	private final Vector4 lightPosition = new Vector4(getXresolution() / 3.5, getYresolution() / 2.5, -200);
	private final Vector4 lightColor = new Vector4(.2, .2, 0.8);

	private final int gridColumns = 633;
	private final int gridRows = 33;

	public SimulationParameters() {
	}
	
	public int getXresolution() {
		return xResolution;
	}

	public int getYresolution() {
		return yResolution;
	}

	public int getFramesleeptime() {
		return frameSleepTime;
	}

	public boolean isRenderalllines() {
		return renderAllLines;
	}

	public boolean isRendersprings() {
		return renderSprings;
	}

	public boolean isRendermasses() {
		return renderMasses;
	}

	public double getMousedragzdepth() {
		return mouseDragZDepth;
	}

	public double getVerticalrestlengthfactor() {
		return verticalRestLengthFactor;
	}

	public double getHorizontalrestlengthfactor() {
		return horizontalRestLengthFactor;
	}

	public boolean isLightsurfaces() {
		return lightSurfaces;
	}

	public Vector4 getLightposition() {
		return lightPosition;
	}

	public Vector4 getLightcolor() {
		return lightColor;
	}

	public int getGridcolumns() {
		return gridColumns;
	}

	public int getGridrows() {
		return gridRows;
	}
}
