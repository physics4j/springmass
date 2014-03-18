package de.codesourcery.springmass.springmass;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import com.badlogic.gdx.math.Vector3;

public interface IRenderPanel {

	public void setSimulator(Simulator simulator);
           
	public void addTo(Container container);
           
	public Vector3 viewToModel(int x, int y);
           
	public Point modelToView(Vector3 vec);
           
	public Point modelToView(Vector3 vec, double scaleX, double scaleY);
           
	public void modelChanged();
	
	/**
	 * 
	 * @param currentFPS average FPS to display
	 * @return <code>true</code> if frame was actually rendered, <code>false</code> if rendering
	 * was suppressed because of VSync limiting
	 */
    public boolean renderFrame(float currentFPS);
	
	public void addKeyListener( KeyListener listener);

	public void addMouseListener( MouseListener listener);
	
	public void addMouseMotionListener( MouseMotionListener listener);

	public void setPreferredSize( Dimension preferredSize);	
}