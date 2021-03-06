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
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;

import javax.swing.JFrame;

import de.codesourcery.springmass.math.Vector4;

public class Main extends Frame {

	private final Object SIMULATOR_LOCK = new Object();

	// @GuardedBy( SIMULATOR_LOCK )
	private Simulator simulator;

	// @GuardedBy( SIMULATOR_LOCK )
	private SimulationParameters parameters;

	private final IRenderPanel renderPanel;

	public static void main(String[] args) 
	{
		new Main(new SimulationParamsBuilder().build() );
	}

	public Main(SimulationParameters parameters) 
	{
		setup( parameters , false );

		renderPanel = new RenderPanel();
		renderPanel.setSimulator( simulator );

		MyKeyListener keyListener = new MyKeyListener();
		renderPanel.addKeyListener( keyListener );

		final MyMouseAdapter mouseAdapter = new MyMouseAdapter();
		renderPanel.addMouseListener( mouseAdapter );
		renderPanel.addMouseMotionListener( mouseAdapter );

		renderPanel.setPreferredSize( new Dimension(800,400 ) );
		
		final JFrame controlFrame = new ControlPanel() {
			protected void applyChanges(SimulationParameters newParameters) {
				setup( newParameters , true );
			}
		}.createFrame();
		
		final WindowAdapter closeListener = new WindowAdapter() 
		{
			public void windowClosing(java.awt.event.WindowEvent e) 
			{
				synchronized(SIMULATOR_LOCK) 
				{
					if ( simulator != null ) {
						simulator.destroy();
					}
					System.exit(0);					
				}
			}
		};
		
		controlFrame.addWindowListener( closeListener );
		addWindowListener( closeListener );
		
        renderPanel.addTo( this );
        pack();
        
		setVisible( true );
		
		controlFrame.setLocation( new Point( getLocation().x + getSize().width , getLocation().y ) );
		controlFrame.setVisible( true );
		
		// sleep some time to avoid a NPE
		// when the Simulation thread triggers a UI refresh
		// while the rendering panel's buffer strategy has not yet been
		// because it's not visible yet
		try {
			Thread.sleep(250);
		} catch (InterruptedException e1) {
		}
		
		synchronized(SIMULATOR_LOCK) 
		{
			this.simulator.start();
		}
	}

	public void setup(SimulationParameters parameters,boolean startSimulator) 
	{
		synchronized(SIMULATOR_LOCK) 
		{
			if ( this.simulator != null ) {
				this.simulator.destroy();
			}

			this.parameters = parameters;
			this.simulator = new Simulator(parameters) {

                @Override
                protected void afterTick()
                {
                    renderPanel.modelChanged();
                }
            };
			
			if ( renderPanel != null ) {
				renderPanel.setSimulator( simulator );
			}
			if ( startSimulator ) {
				this.simulator.start();
			}
		}
	}

	protected final class MyMouseAdapter extends MouseAdapter 
	{
		private Mass selected;

		private Mass getNearestMass(int x,int y) 
		{
			synchronized(SIMULATOR_LOCK) 
			{
				final SimulationParameters params = simulator.getSimulationParameters();
				
				final double gridWidth= params.getXResolution() / params.getGridColumnCount();
				final double gridHeight = params.getYResolution() / params.getGridRowCount();
				final double pickDepth = Math.abs( params.getMouseDragZDepth() + 1 );
				
				final double radiusSquared = gridWidth*gridWidth + gridHeight*gridHeight + pickDepth*pickDepth;
				
				final Vector4 mousePointer = renderPanel.viewToModel( x, y );
				return simulator.getSpringMassSystem().getNearestMass( mousePointer , radiusSquared );
			}
		}
		
		public void mousePressed(java.awt.event.MouseEvent e) 
		{
			synchronized(SIMULATOR_LOCK) 
			{
				final int button = e.getButton();

				if ( button == MouseEvent.BUTTON1 || button == MouseEvent.BUTTON3 ) 
				{ 
					final Mass nearest = getNearestMass( e.getX() , e.getY() );
					if ( button == MouseEvent.BUTTON1 ) // left click 
					{
						setSelected( nearest );
					} 
					else  // right click
					{
						if ( nearest != null ) 
						{
							nearest.setFixed( ! nearest.isFixed() );
		                    renderPanel.modelChanged();							
						}
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
			renderPanel.modelChanged();			
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
					renderPanel.modelChanged();					
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