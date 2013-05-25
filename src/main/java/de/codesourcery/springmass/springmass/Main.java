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

import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;

import de.codesourcery.springmass.math.Vector4;

public class Main extends JFrame {

	private final Object SIMULATOR_LOCK = new Object();

	// @GuardedBy( SIMULATOR_LOCK )
	private Simulator simulator;

	// @GuardedBy( SIMULATOR_LOCK )
	private SimulationParameters parameters;

	private final RenderPanel renderPanel;

	public static void main(String[] args) 
	{
		new Main(new SimulationParameters());
	}

	public Main(SimulationParameters parameters) 
	{
		setup( parameters );

		renderPanel = new RenderPanel();
		renderPanel.setSimulator( simulator );

		MyKeyListener keyListener = new MyKeyListener();
		renderPanel.addKeyListener( keyListener );

		final MyMouseAdapter mouseAdapter = new MyMouseAdapter();
		renderPanel.addMouseListener( mouseAdapter );
		renderPanel.addMouseMotionListener( mouseAdapter );

		renderPanel.setPreferredSize( new Dimension(800,400 ) );
		getContentPane().add( renderPanel );
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setVisible( true );
	}

	public void setup(SimulationParameters parameters) 
	{
		synchronized(SIMULATOR_LOCK) 
		{
			if ( this.simulator != null ) {
				this.simulator.destroy();
			}

			this.parameters = parameters;
			this.simulator = new Simulator(parameters) 
			{
				@Override
				protected void afterTick() 
				{
					renderPanel.doRender();
				}
			};		
			if ( renderPanel != null ) {
				renderPanel.setSimulator( simulator );
			}
		}
	}

	protected final class MyMouseAdapter extends MouseAdapter 
	{
		private Mass selected;

		public void mousePressed(java.awt.event.MouseEvent e) 
		{
			synchronized(SIMULATOR_LOCK) 
			{
				final SpringMassSystem system = simulator.getSpringMassSystem();

				final int button = e.getButton();

				if ( button == MouseEvent.BUTTON1 || button == MouseEvent.BUTTON3 ) 
				{ 
					// left click
					final Vector4 mousePointer = renderPanel.viewToModel( e.getX() , e.getY() );
					final Mass nearest = system.getNearestMass( mousePointer , 5*5 );
					if ( button == MouseEvent.BUTTON1 ) 
					{
						setSelected( nearest );
						renderPanel.repaint();
					} 
					else 
					{
						if ( nearest != null ) 
						{
							nearest.setFixed( ! nearest.isFixed() );
							renderPanel.repaint();
						}
					}
				} 
				else if ( button == MouseEvent.BUTTON2 ) 
				{
					final Vector4 mousePointer = renderPanel.viewToModel( e.getX() , e.getY() );

					final Mass nearest = system.getNearestMass( mousePointer , 25*25 );

					if ( nearest != null ) 
					{
						final Vector4 edge = nearest.currentPosition.minus(new Vector4(1,0,0 ) );
						for ( Spring s : system.getIntersectingSprings( edge.x , nearest.currentPosition , 100 ) )
						{
							system.removeSpring( s);
						}
						renderPanel.repaint();
					}
				}
			}
		}

		private void setSelected(Mass m) 
		{
			if ( selected != null && selected != m ) 
			{
				selected.setSelected(false);
			}
			selected = m;
			if ( selected != null ) {
				selected.setSelected( true );
			}
		}

		public void mouseReleased(MouseEvent e) 
		{
			if ( e.getButton() == MouseEvent.BUTTON1 ) 
			{ 
				setSelected(null);
			}
		}

		public void mouseDragged(MouseEvent e) 
		{
			if ( selected != null ) 
			{
				synchronized(SIMULATOR_LOCK) 
				{
					final Vector4 newPos = renderPanel.viewToModel( e.getX() , e.getY() );
					newPos.z=parameters.getMouseDragZDepth();
					selected.setPosition( newPos );
					renderPanel.repaint();
				}
			}
		}
	};	

	protected final class MyKeyListener extends KeyAdapter 
	{
		@Override
		public void keyTyped(java.awt.event.KeyEvent e) 
		{
			if ( e.getKeyChar() == 's' ) 
			{
				synchronized(SIMULATOR_LOCK) 
				{
					if ( simulator.isRunning() ) 
					{
						simulator.stop();
					} else {
						simulator.start();
					}
				}
			}
		}
	}	
}