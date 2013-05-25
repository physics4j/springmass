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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.codesourcery.springmass.springmass.SimulationParamsBuilder.SimulationParameter;

public class ControlPanel extends JPanel {

	private final SimulationParamsBuilder builder;
	private volatile SimulationParameters parameters;
	
	private final Map<SimulationParameter,JComponent> components = new HashMap<>();
	
	protected static final class ValueConverter {
		
		private final SimulationParameter p;
		
		public ValueConverter(final SimulationParameter p) {
			this.p = p;
		}
		
		public boolean isValid(String value) {
			return true;
		}
		
		public void apply(Object value) 
		{
			System.out.println("Setting "+p.getName() +" = "+value);
			
			if ( value instanceof String) 
			{
				p.setValue( convertString( (String) value ) );
			} else {
				p.setValue( value );
			}
		}
		
		private boolean isNumeric(Class<?> clazz) {
			if ( Number.class.isAssignableFrom( clazz ) ) {
				return true;
			}
			if ( clazz.isPrimitive() ) {
				return clazz  == Long.TYPE || clazz == Integer.TYPE || clazz == Double.TYPE || clazz == Float.TYPE;
			}
			return false;
		}

		private Object convertString(String value) 
		{
			if ( isNumeric( p.getType() ) ) 
			{
				double val = Double.parseDouble( value );
				if ( p.getType() == Long.class || p.getType() == Long.TYPE ) {
					return (long) val;
				} 
				
				if ( p.getType() == Integer.class || p.getType() == Integer.TYPE ) 
				{
					return (int) val;
				} 
				
				if ( p.getType() == Double.class || p.getType() == Double.TYPE ) {
					return val;
				} 
				
				if ( p.getType() == Float.class || p.getType() == Float.TYPE ) {
					return (float) val;
				} 
				throw new RuntimeException("Unhandled numeric type: "+p);
			}
			
			if ( p.getType() == Boolean.class || p.getType() == Boolean.TYPE ) {
				return Boolean.parseBoolean( value.toLowerCase() );
			}
			throw new RuntimeException("Unhandled parameter type: "+p);
		}
	}
	
	public static void main(String[] args) 
	{
		JFrame frame = new ControlPanel().createFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE );
		frame.setVisible(true);
	}

	public JFrame createFrame() 
	{
		final JFrame frame = new JFrame("Simulation parameters");

		GridBagConstraints cnstrs = new GridBagConstraints();
		cnstrs.fill=GridBagConstraints.BOTH;
		cnstrs.gridx=0;
		cnstrs.gridy=0;
		cnstrs.gridheight=GridBagConstraints.REMAINDER;
		cnstrs.gridwidth=GridBagConstraints.REMAINDER;
		cnstrs.weightx=1.0;
		cnstrs.weighty=1.0;
		
		frame.getContentPane().setLayout( new GridBagLayout() );
		
		frame.getContentPane().add( this  , cnstrs );
		
		frame.pack();
		return frame;
	}
	
	protected void applyChanges(SimulationParameters newParameters) 
	{
		
	}

	public ControlPanel() 
	{
		builder = new SimulationParamsBuilder();
		parameters = builder.build();
		setup();
	}
	
	private void setup() 
	{
		// setup input panel
		final List<SimulationParameter> params = builder.getParameters();
		final JPanel inputPanel = new JPanel();
		inputPanel.setLayout( new GridLayout( params.size() , 2 ) );
		for ( SimulationParameter p : params ) 
		{
			final JComponent component = createComponent(p);
			inputPanel.add( new JLabel( p.getName() ) );
			inputPanel.add( component );
			components.put( p , component );
		}
		
		setLayout( new GridBagLayout() );
		
		// add input panel
		GridBagConstraints cnstrs = new GridBagConstraints();
		cnstrs.fill=GridBagConstraints.HORIZONTAL;
		cnstrs.gridx=0;
		cnstrs.gridy=0;		
		cnstrs.gridheight=GridBagConstraints.RELATIVE;
		cnstrs.gridwidth=GridBagConstraints.REMAINDER;
		cnstrs.weightx=1;
		cnstrs.weighty=0;
		add( inputPanel , cnstrs );
		
		// add apply button
		cnstrs = new GridBagConstraints();
		cnstrs.fill=GridBagConstraints.NONE;
		cnstrs.gridx=0;
		cnstrs.gridy=1;		
		cnstrs.gridheight=GridBagConstraints.REMAINDER;
		cnstrs.gridwidth=GridBagConstraints.REMAINDER;
		cnstrs.weightx=1;
		cnstrs.weighty=1;
		
		final JButton applyButton = new JButton("Apply");
		applyButton.addActionListener( new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				parameters = builder.build();
				applyChanges( parameters );
			}
		});

		add( applyButton , cnstrs );
	}
	
	private JComponent createComponent(final SimulationParameter p) 
	{
		final ValueConverter converter = new ValueConverter(p);
		
		final JComponent result;
		if ( p.getType() == Boolean.class || p.getType() == Boolean.TYPE ) 
		{
			if ( p.isWriteOnly() ) 
			{
				final JButton tmp = new JButton( "Trigger" );
				result = tmp;				
			} 
			else 
			{
				final JCheckBox tmp = new JCheckBox();
				tmp.addActionListener( new ActionListener() 
				{
					@Override
					public void actionPerformed(ActionEvent e) {
						converter.apply( tmp.isSelected() );
					}
				});				
				final Object value = p.getValue();
				tmp.setSelected( value == null ? false : (Boolean) value);
				if ( p.isReadOnly() ) 
				{
					tmp.setEnabled( false );
				}
				result = tmp;
			}
		} 
		else 
		{
			final JTextField tmp = new JTextField();
			tmp.addActionListener( new ActionListener() 
			{
				@Override
				public void actionPerformed(ActionEvent e) {
					converter.apply( tmp.getText() );
				}
			});
			tmp.addFocusListener( new FocusListener() {
				
				@Override
				public void focusLost(FocusEvent e) {
					converter.apply( tmp.getText() );					
				}
				
				@Override
				public void focusGained(FocusEvent e) {
				}
			});
			final Object value = p.getValue();
			tmp.setText( value == null ? "" : value.toString() );
			if ( p.isReadOnly() ) 
			{
				tmp.setEditable( false );
				tmp.setEnabled( false );
			}
			result = tmp;
		}
		return result;
	}
	
	public SimulationParameters getSimulationParameters() {
		return parameters;
	}
}