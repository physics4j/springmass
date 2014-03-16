package de.codesourcery.springmass.springmass;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import de.codesourcery.springmass.math.Vector4;

public interface IRenderPanel {

	public void setSimulator(Simulator simulator);
           
	public void addTo(Container container);
           
	public Vector4 viewToModel(int x, int y);
           
	public Point modelToView(Vector4 vec);
           
	public Point modelToView(Vector4 vec, double scaleX, double scaleY);
           
	public void modelChanged();
	
	public void addKeyListener( KeyListener listener);

	public void addMouseListener( MouseListener listener);
	
	public void addMouseMotionListener( MouseMotionListener listener);

	public void setPreferredSize( Dimension preferredSize);	
}