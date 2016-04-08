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