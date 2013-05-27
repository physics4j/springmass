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
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.codesourcery.springmass.springmass.SimulationParamsBuilder.SimulationParameter;
import de.codesourcery.springmass.springmass.SimulationParamsBuilder.SliderHint;

public abstract class ControlPanel extends JPanel {

	private static final int SLIDER_MAX = 1000;
	
	private final SimulationParamsBuilder builder;
	private volatile SimulationParameters parameters;
	
	private final Map<SimulationParameter,JComponent> components = new HashMap<>();
	
	protected static final class ValueConverter {
		
		private final SimulationParameter p;
		
		public ValueConverter(final SimulationParameter p) {
			this.p = p;
		}
		
		public SimulationParameter getSimulationParameter() {
			return p;
		}
		
		public void apply(Object value) 
		{
//			System.out.println("Setting "+p.getName() +" = "+value);
			
			if ( value instanceof String) 
			{
				p.setValue( convertString( (String) value ) );
			} 
			else 
			{
				if ( p.isNumericParameter() ) 
				{
					Object realValue = value;
					if ( p.getType() == Double.class || p.getType() == Double.TYPE ) {
						realValue = ((Number) value).doubleValue();
					} else if ( p.getType() == Float.class || p.getType() == Float.TYPE ) {
						realValue = ((Number) value).floatValue();
					} else if ( p.getType() == Long.class || p.getType() == Long.TYPE ) {
						realValue = ((Number) value).longValue();
					} else if ( p.getType() == Integer.class || p.getType() == Integer.TYPE ) {
						realValue = ((Number) value).intValue();
					} else if ( p.getType() == Short.class || p.getType() == Short.TYPE ) {
						realValue = ((Number) value).shortValue();
					} else if ( p.getType() == Byte.class || p.getType() == Byte.TYPE ) {
						realValue = ((Number) value).byteValue();
					}
					p.setValue( realValue );
					return;
				}
				p.setValue( value );
			}
		}
		
		private Object convertString(String value) 
		{
			if ( p.isNumericParameter() ) 
			{
				double val = Double.parseDouble( value );
				if ( p.getType() == Long.class || p.getType() == Long.TYPE ) {
					return (long) val;
				} 
				
				if ( p.getType() == Integer.class || p.getType() == Integer.TYPE ) 
				{
					return (int) val;
				} 
				
				if ( p.getType() == Short.class || p.getType() == Short.TYPE ) 
				{
					return (short) val;
				}	
				
				if ( p.getType() == Byte.class || p.getType() == Byte.TYPE ) 
				{
					return (short) val;
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
		final JFrame frame = new ControlPanel() 
		{
			@Override
			protected void applyChanges(SimulationParameters p) {}
		}.createFrame();
		
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
	
	protected abstract void applyChanges(SimulationParameters newParameters); 

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
		
		populateInputPanel(params, inputPanel);
		
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
		cnstrs.gridwidth=GridBagConstraints.RELATIVE;
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
		
		// add reset button
		cnstrs = new GridBagConstraints();
		cnstrs.fill=GridBagConstraints.NONE;
		cnstrs.gridx=1;
		cnstrs.gridy=1;		
		cnstrs.gridheight=GridBagConstraints.REMAINDER;
		cnstrs.gridwidth=GridBagConstraints.REMAINDER;
		cnstrs.weightx=1;
		cnstrs.weighty=1;
		
		final JButton resetButton = new JButton("Reset");
		resetButton.addActionListener( new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				builder.reset();
				parameters = builder.build();
				inputPanel.removeAll();
				populateInputPanel( builder.getParameters() , inputPanel );
				revalidate();
				applyChanges( parameters );
			}
		});		
		add( resetButton , cnstrs );
	}

	private void populateInputPanel(final List<SimulationParameter> params,final JPanel inputPanel) 
	{
		for ( SimulationParameter p : params ) 
		{
			final JComponent component = createComponent(p);
			inputPanel.add( new JLabel( p.getName() ) );
			inputPanel.add( component );
			components.put( p , component );
		}
	}
	
	private JComponent createComponent(final SimulationParameter p) 
	{
		final ValueConverter converter = new ValueConverter(p);
		
		final JComponent result;
		if ( p.isNumericParameter() && ! p.getHints( SliderHint.class ).isEmpty() ) 
		{
			result = createNumericInput( converter , p.getHints(SliderHint.class).get(0) ); 
		} 
		else if ( p.getType() == Color.class ) 
		{
			final JButton tmp = new JButton( "    " );
			tmp.addActionListener( new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) 
				{
					Color result = JColorChooser.showDialog(null, p.getName() , (Color) p.getValue() );
					if ( result != null ) {
						p.setValue( result );
						tmp.setForeground( result );
						tmp.setBackground( result );
					}
				}
			});
			final Color c = (Color) p.getValue();
			tmp.setForeground( c );
			tmp.setBackground( c );
			result = tmp;	
		} 
		else if ( p.getType() == Boolean.class || p.getType() == Boolean.TYPE ) 
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
	
	
	protected static final class SliderHelper {
		
		private final SliderHint hint;
		private final double g;
		private final SimulationParameter parameter;
		
		public SliderHelper(SimulationParameter p , SliderHint hint) {
			this.hint = hint;
			this.parameter = p;
			g = hint.getMaxValue() - hint.getMinValue();
		}
		
		public double fromSliderValue(int sliderValue) 
		{
	    	final double result = hint.getMinValue() + (sliderValue/(double) SLIDER_MAX)*g;
	    	return result;
		}
		
		public int toSliderValue(double value) 
		{
			final double actualValue = value - hint.getMinValue();

			int sliderPos = (int) Math.round( SLIDER_MAX*( actualValue / g ) );
			if ( sliderPos > SLIDER_MAX ) {
				sliderPos = SLIDER_MAX;
			} else if ( sliderPos < 0 ) {
				sliderPos = 0;
			}
			return sliderPos;			
		}
	}
	
	private JComponent createNumericInput(final ValueConverter valueConverter,final SliderHint hint) 
	{
		final SimulationParameter p = valueConverter.getSimulationParameter();
		
		final JTextField textfield = new JTextField();
		
		final SliderHelper helper = new SliderHelper( p , hint );
		
		final int sliderValue = helper.toSliderValue( ((Number) p.getValue() ).doubleValue() );
		final JSlider slider = new JSlider( JSlider.HORIZONTAL, 0 , SLIDER_MAX , sliderValue );
		
		final ChangeListener[] changeListener={null};
		final ActionListener textFieldListener = new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				double value = Double.parseDouble( textfield.getText() );
				if ( p.isIntegerParameter() ) {
					value = Math.round(value);
				}
				valueConverter.apply( value );
				
				slider.getModel().removeChangeListener( changeListener[0] );
				try {
					slider.setValue( helper.toSliderValue( value ) );
				} 
				finally {
					slider.getModel().addChangeListener( changeListener[0] );
				}
			}
		};
		
		changeListener[0] = new ChangeListener() 
		{
			@Override
			public void stateChanged(ChangeEvent e) 
			{
		    	double newValue = helper.fromSliderValue( slider.getValue() );
		    	if ( p.isIntegerParameter()  ) {
		    		newValue = Math.round(newValue);
		    		valueConverter.apply( newValue );
		    	} else {
		    		valueConverter.apply( newValue );
		    	}
		    	
		    	textfield.removeActionListener( textFieldListener );
		    	try 
		    	{
		    		if ( p.isIntegerParameter() ) {
		    			textfield.setText( Long.toString( (long) newValue ) );
		    		} else {
		    			textfield.setText( Double.toString( newValue ) );
		    		}
		    	} finally {
		    		textfield.addActionListener(textFieldListener);
		    	}
			}
		};		
		
		// setup slider
		slider.getModel().addChangeListener( changeListener[0] );
		
		// setup textfield
		textfield.setText( p.getValue().toString() );
		textfield.setColumns( 6 );
		textfield.setHorizontalAlignment(JTextField.TRAILING);
		textfield.addActionListener( textFieldListener );
		textfield.addFocusListener( new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
				valueConverter.apply( textfield.getText() );					
			}
			
			@Override
			public void focusGained(FocusEvent e) {
			}
		});
		
		if ( p.isReadOnly() ) 
		{
			slider.setEnabled( false );
			textfield.setEditable( false );
			textfield.setEnabled( false );
		}
		
		// setup panel
		final JPanel panel = new JPanel();
		panel.setLayout( new GridBagLayout() );		
		
		// add textfield
		GridBagConstraints cnstrs = new GridBagConstraints();
		cnstrs.fill=GridBagConstraints.HORIZONTAL;
		cnstrs.gridx=0;
		cnstrs.gridy=0;		
		cnstrs.gridheight=GridBagConstraints.REMAINDER;
		cnstrs.gridwidth=GridBagConstraints.RELATIVE;
		cnstrs.weightx=0.1;
		cnstrs.weighty=0;	
		panel.add( textfield , cnstrs );	
		
		// add slider
		cnstrs = new GridBagConstraints();
		cnstrs.fill=GridBagConstraints.HORIZONTAL;
		cnstrs.gridx=1;
		cnstrs.gridy=0;		
		cnstrs.gridheight=GridBagConstraints.REMAINDER;
		cnstrs.gridwidth=GridBagConstraints.REMAINDER;
		cnstrs.weightx=0.9;
		cnstrs.weighty=0;	
		panel.add( slider , cnstrs );		
		
		return panel;
	}
	
	public SimulationParameters getSimulationParameters() {
		return parameters;
	}
}