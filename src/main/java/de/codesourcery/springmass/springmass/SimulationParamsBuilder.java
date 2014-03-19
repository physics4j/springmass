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
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import com.badlogic.gdx.math.Vector3;

public class SimulationParamsBuilder {

	/* 
	 *  See reset() method for default values.
	 */
	
	private int xResolution;
	private int yResolution;

	private float desiredFPS;
	
	private boolean waitForVSync;
	
	private boolean debugPerformance;
	
	private float springDampening;
	private float springCoefficient;

	private boolean renderAllSprings;

	private float maxSpringLength;
	
	private boolean renderSprings;

	private boolean renderMasses;
	
	private float particleMass;

	private float mouseDragZDepth;

	private float verticalRestLengthFactor; 
	private float horizontalRestLengthFactor;
	
	private final WindParameters windParameters = new WindParameters();

	private boolean lightSurfaces;
	
	private Vector3 lightPosition;
	private Color lightColor;

	private float gravity;

	private int gridColumnCount;
	private int gridRowCount;

	private float maxParticleSpeed;
	private int forkJoinBatchSize;
	
	private float integrationTimeStep;
	private int iterationCount;
	
	public static interface Hint {
	}
	
	public static final class SliderHint implements Hint
	{
		private final float minValue;
		private final float maxValue;
		
		protected SliderHint(float minValue, float maxValue) 
		{
			this.minValue = minValue;
			this.maxValue = maxValue;
		}
		
		@Override
		public String toString() {
			return "SliderHint [minValue=" + minValue + ", maxValue="+ maxValue + "]";
		}

		public float getMinValue() { return minValue; }
		public float getMaxValue() { return maxValue; }
	}
	
	/**
	 * Method that is never considered a parameter getter/setter.
	 *
	 * @author tobias.gierke@code-sourcery.de
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface IgnoreMethod { }
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface ValueRange
	{ 
		public float minValue();
		public float maxValue();
	}	
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface Label
	{ 
		public String value();
	}		
	
	/**
	 * A simulation parameter.
	 *
	 * @author tobias.gierke@code-sourcery.de
	 */
	public abstract class SimulationParameter 
	{
		public abstract String getName();
		
		public abstract Class<?> getType();
		
		public abstract Object getValue();
		
		public abstract boolean isReadOnly();
		
		public abstract boolean isWriteOnly();
		
		public abstract void setValue(Object value);
		
	    public final boolean isNumericParameter() {
	        return isNumeric( getType() );
	    }
	    
	    public final boolean isIntegerParameter() 
	    {
	        if ( isNumericParameter() ) 
	        {
	            Class<?> clazz = getType();
	            if ( clazz == Long.class || clazz == Integer.class || clazz == Short.class || clazz == Byte.class) {
	                return true;
	            }
	            if ( clazz == Long.TYPE || clazz == Integer.TYPE || clazz == Short.TYPE || clazz == Byte.TYPE ) {
	                return true;
	            }
	        }
	        return false;
	    }
	    
	    private final boolean isNumeric(Class<?> clazz) 
	    {
	        if ( Number.class.isAssignableFrom( clazz ) ) {
	            return true;
	        }
	        if ( clazz.isPrimitive() ) {
	            return clazz  == Long.TYPE || clazz == Integer.TYPE || clazz == Double.TYPE || clazz == Float.TYPE || clazz == Short.TYPE || clazz == Byte.TYPE;
	        }
	        return false;
	    }   
	    
		@SuppressWarnings("unchecked")
		public final <T extends Hint> List<T> getHints(Class<T> clazz) 
		{
			final List<T> result = new ArrayList<>();
			for ( Hint h : getHints() ) {
				if ( clazz.isAssignableFrom( h.getClass() ) ) {
					result.add( (T) h );
				}
			}			
			return result;
		}
		
		public List<Hint> getHints() {
			return Collections.emptyList();
		}
		
		public double getMinValue() {
			return -Double.MAX_VALUE;
		}
		
		public double getMaxValue() {
			return Double.MAX_VALUE;
		}		
		
		@Override
		public final boolean equals(Object obj) 
		{
			if ( obj instanceof SimulationParameter) {
				return getName().equals( ((SimulationParameter) obj).getName() );
			}
			return false;
		}
		
		@Override
		public final int hashCode() {
			return getName().hashCode();
		}
	}
	
	public SimulationParamsBuilder() {
		reset();
	}
	
	public void reset() 
	{
		xResolution = 1000;
		yResolution = 1000;

		gridColumnCount = 66;
		gridRowCount = 44;
		
		gravity = 30;
		maxParticleSpeed = 40;
		
		particleMass = 1.0f;
		springDampening=3;
		springCoefficient=0.3f;	
		
		maxSpringLength = -1;		
		verticalRestLengthFactor = 0.4f;
		horizontalRestLengthFactor = 1;		

		lightSurfaces = true;
		lightColor = new Color(50, 50, 200);
		renderAllSprings = false;
		renderSprings = false;
		renderMasses = false;
		
		mouseDragZDepth = -100;

		integrationTimeStep = 50;
		iterationCount = 5;		
		forkJoinBatchSize = 250;	
		
		debugPerformance=false;		
		desiredFPS = 60;
		waitForVSync = true;		
		
		final float oneDegree = (float) (2f*Math.PI)/360.0f;
		final float maxAngle = (float) (2*Math.PI - oneDegree);
		this.windParameters.setMinAngle( new SphericalCoordinates( 0 , 0 ) );
		this.windParameters.setMaxAngle( new SphericalCoordinates( maxAngle , maxAngle ) );
		
		this.windParameters.setMinForce( 30 );
		this.windParameters.setMaxForce( 60 );
		
		this.windParameters.setStepsUntilDirectionAdjusted( 60 );
		this.windParameters.setStepsUntilDirectionChanged( 60 );
		
		this.windParameters.setEnabled(true);
		
		updateLightPosition();
	}
	
	public static void main(String[] args) {
		final List<SimulationParameter> parameters = new SimulationParamsBuilder().getParameters();
		System.out.println( StringUtils.join( parameters , "\n" ) );
	}

	public SimulationParameters build() 
	{
		return new SimulationParameters(xResolution, yResolution, 
				desiredFPS, renderAllSprings, renderSprings, renderMasses, mouseDragZDepth, 
				verticalRestLengthFactor, horizontalRestLengthFactor, 
				lightSurfaces, lightPosition, lightColor, gravity, 
				gridColumnCount, gridRowCount, maxParticleSpeed, forkJoinBatchSize,springCoefficient , springDampening,particleMass,debugPerformance,
				integrationTimeStep,maxSpringLength,getIterationCount(),waitForVSync , windParameters );
	}
	
	public float getMaxSpringLength() {
		return maxSpringLength;
	}
	
	@ValueRange(minValue=-1,maxValue=200)
	public void setMaxSpringLength(float maxSpringLength) {
		this.maxSpringLength = maxSpringLength;
	}
	
	public float getIntegrationTimeStep() {
		return integrationTimeStep;
	}
	
	@ValueRange(minValue=2,maxValue=200)
	public void setIntegrationTimeStep(float integrationTimeStep) {
		this.integrationTimeStep = integrationTimeStep;
	}
	
	@IgnoreMethod
	public List<SimulationParameter> getParameters() 
	{
		final List<SimulationParameter> result = new ArrayList<>();
		
		final Set<String> parameterNames = new HashSet<>();
		final Map<String,Method> setterMethods=new HashMap<>();
		final Map<String,Method> getterMethods=new HashMap<>();
		
		for ( Method m : getClass().getDeclaredMethods() ) 
		{
			if ( m.getAnnotation( IgnoreMethod.class ) != null ) {
				continue;
			}
			
			if ( isParameterSetter(m ) ) {
				final String paramName = extractParameterName( m );
				parameterNames.add( paramName );
				setterMethods.put( paramName , m );
			} else if ( isParameterGetter(m) ) 
			{
				final String paramName = extractParameterName( m );
				parameterNames.add( paramName );
				getterMethods.put( paramName , m );				
			}
		}
		
		for ( final String paramName : parameterNames ) 
		{
			final Method getter = getterMethods.get(paramName);
			final Method setter = setterMethods.get(paramName);
			
			final Class<?> getterType = getter != null ? getter.getReturnType() : null;
			final Class<?> setterType = setter != null ? setter.getParameterTypes()[0] : null;
			
			if ( getter != null && setter != null && ! ObjectUtils.equals( getterType,setterType ) ) 
			{
				throw new RuntimeException("Internal error, different return/parameter types for getter/setter methods "+setter+" and "+getter);
			}
			
			final List<Hint> hints = new ArrayList<>();
			hints.addAll( setter != null ? getParameterHints( setter ) : Collections.<Hint>emptySet() );
			if ( getter != null && ! getParameterHints( getter ).isEmpty() ) {
				throw new RuntimeException("@ValueRange annotation may only be placed on setter methods, offender: "+getter);
			}
			
			final Class<?> type = getterType != null ? getterType : setterType;
			
			result.add( new SimulationParameter() 
			{
				@Override
				public boolean isReadOnly() 
				{
					return setter == null;
				}
				
				@Override
				public boolean isWriteOnly() {
					return getter == null;
				}
				
				@Override
				public List<Hint> getHints() {
					return hints;
				}
				
				@Override
				public String toString() 
				{
					String readOnly= isReadOnly() ? "READ-ONLY" : "";
					String writeOnly= isWriteOnly() ? "WRITE-ONLY" : "";
							
					return "Parameter '"+getName()+"' , Type "+getType()+" , flags: "+readOnly+" "+writeOnly;
				}
				
				@Override
				public void setValue(Object value) 
				{
					if ( isReadOnly() ) {
						throw new UnsupportedOperationException("Cannot set value of read-only parameter '"+getName()+"'");
					}
					try {
						setter.invoke( SimulationParamsBuilder.this , value );
					} 
					catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) 
					{
						if ( e instanceof IllegalArgumentException ) {
							throw new IllegalArgumentException("Method "+setter+", got: "+value.getClass()+" , expected: "+setter.getParameterTypes()[0] );
						}
						throw new RuntimeException(e);
					}
				}
				
				@Override
				public Object getValue() 
				{
					if ( isWriteOnly() ) {
						throw new UnsupportedOperationException("Cannot read value of write-only parameter '"+getName()+"'");
					}					
					try {
						return getter.invoke( SimulationParamsBuilder.this );
					} 
					catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) 
					{
						throw new RuntimeException(e);						
					}
				}
				
				@Override
				public Class<?> getType() 
				{
					return type;
				}
				
				@Override
				public String getName() {
					return paramName;
				}
			});
		}
		
		Collections.sort( result , new Comparator<SimulationParameter>() {

			@Override
			public int compare(SimulationParameter o1, SimulationParameter o2) 
			{
				return o1.getName().compareTo(o2.getName());
			}} );
		return result;
	}
	
	private List<Hint> getParameterHints(Method m) 
	{
		final List<Hint> result = new ArrayList<>();
		ValueRange hint = m.getAnnotation(ValueRange.class);
		if ( hint != null ) 
		{
			float minValue = hint.minValue();
			float maxValue = hint.maxValue();
			if ( minValue > maxValue ) {
				throw new IllegalArgumentException("minValue > maxValue in @ValueRange annotation on "+m);
			}
			result.add( new SliderHint( minValue , maxValue ) );
		}
		return result;
	}
	
	private String extractParameterName(Method m) 
	{
		Method methodWithLabel = null;
		if ( isParameterGetter( m ) ) {
			// look for @Label on corresponding setter
			if ( m.getName().startsWith("get") || m.getName().startsWith("is") ) 
			{
				final int charsToRemove = m.getName().startsWith("get") ? 3 : 2;
				final String setterName = "set"+m.getName().substring( charsToRemove );
				try {
					methodWithLabel = m.getDeclaringClass().getMethod( setterName , new Class<?>[] { m.getReturnType() } );
				} 
				catch (NoSuchMethodException | SecurityException e) { /* ok */ }
			} 
		} else {
			methodWithLabel = m;
		}
		
		if ( methodWithLabel != null ) {
			final Label label = methodWithLabel.getAnnotation(Label.class);
			if ( label != null && ! StringUtils.isBlank( label.value() ) ) {
				return label.value();
			}			
		}
		
		String name;
		if ( m.getName().startsWith("set") ||  m.getName().startsWith("get")) {
			name = m.getName().substring(3);
		} 
		else if ( m.getName().startsWith("is" ) ) 
		{
			name = m.getName().substring(2);
		} else {
			throw new IllegalArgumentException("Invalid method name: "+m); 
		}
		name = Character.toUpperCase( name.charAt(0) )+name.substring(1);
		final List<String> split = new ArrayList<>();
		final StringBuffer buffer = new StringBuffer();
		for ( char s : name.toCharArray() ) 
		{
			if ( Character.isUpperCase( s ) ) 
			{
				if ( buffer.length() > 1 ) {
					split.add( buffer.toString() );
					buffer.setLength( 0 );
					buffer.append( Character.toLowerCase(s) );
					continue;
				}
			}
			if ( split.isEmpty() ) {
				buffer.append( s );
			} else {
				buffer.append( Character.toLowerCase( s ) );
			}
		}
		if ( buffer.length() > 0 ) {
			split.add( buffer.toString() );
		}
		
		return StringUtils.join( split , " " );
	}
	
	private boolean isParameterSetter(Method m) 
	{
		return isPartOfPublicInterface(m) && 
				m.getParameterTypes() != null && m.getParameterTypes().length == 1 && 
				m.getName().startsWith("set") &&
				m.getReturnType() == Void.TYPE;
	}
	
	private boolean isPartOfPublicInterface(Method m) {
		final int mods = m.getModifiers();
		if ( Modifier.isStatic( mods ) || Modifier.isAbstract( mods ) || Modifier.isNative(mods) || ! Modifier.isPublic( mods ) )
		{
			return false;
		}
		return true;
	}
	
	private boolean isParameterGetter(Method m) 
	{
		return isPartOfPublicInterface(m) &&
			   ( m.getName().startsWith("get") || m.getName().startsWith("is") ) && 
			   m.getReturnType() != Void.TYPE &&
			   m.getParameterTypes() != null && m.getParameterTypes().length == 0;
	}	

	public int getxResolution() {
		return xResolution;
	}
	
	private void updateLightPosition() 
	{
		lightPosition = new Vector3(xResolution / 2f , yResolution / 2.5f, -200);		
	}

	public void setResolution(int xResolution,int yResolution) 
	{
		this.xResolution = xResolution;
		this.yResolution = yResolution;
		updateLightPosition();
	}

	public int getyResolution() {
		return yResolution;
	}

	public float getDesiredFrameRate() {
		return desiredFPS;
	}

    @ValueRange(minValue=0.0f,maxValue=300.0f)
	public void setDesiredFrameRate(float desiredFPS) {
		this.desiredFPS = desiredFPS;
	}

	public boolean isRenderAllSpring() {
		return renderAllSprings;
	}

	public void setRenderAllSpring(boolean renderAllSprings) {
		this.renderAllSprings = renderAllSprings;
	}

	public boolean isRenderSprings() {
		return renderSprings;
	}

	public void setRenderSprings(boolean renderSprings) {
		this.renderSprings = renderSprings;
	}

	public boolean isRenderMasses() {
		return renderMasses;
	}

	public void setRenderMasses(boolean renderMasses) {
		this.renderMasses = renderMasses;
	}

	public float getMouseDragZDepth() {
		return mouseDragZDepth;
	}

	@ValueRange(minValue=-200,maxValue=0)
	public void setMouseDragZDepth(float mouseDragZDepth) {
		this.mouseDragZDepth = mouseDragZDepth;
	}

	public float getVerticalRestLengthFactor() {
		return verticalRestLengthFactor;
	}

	@ValueRange(minValue=0.01f,maxValue=5.0f)	
	public void setVerticalRestLengthFactor(float verticalRestLengthFactor) {
		this.verticalRestLengthFactor = verticalRestLengthFactor;
	}

	public float getHorizontalRestLengthFactor() {
		return horizontalRestLengthFactor;
	}

	@ValueRange(minValue=0.01f,maxValue=5.0f)
	public void setHorizontalRestLengthFactor(float horizontalRestLengthFactor) {
		this.horizontalRestLengthFactor = horizontalRestLengthFactor;
	}

	public boolean isLightSurfaces() {
		return lightSurfaces;
	}

	public void setLightSurfaces(boolean lightSurfaces) {
		this.lightSurfaces = lightSurfaces;
	}

	public Vector3 getLightPosition() {
		return lightPosition;
	}

	public void setLightPosition(Vector3 lightPosition) {
		this.lightPosition = lightPosition;
	}

	public Color getLightColor() {
		return lightColor;
	}

	public void setLightColor(Color lightColor) {
		this.lightColor = lightColor;
	}

	public float getGravity() {
		return gravity;
	}

	@ValueRange(minValue=0,maxValue=100)
	public void setGravity(float gravity) {
		this.gravity = gravity;
	}

	public int getGridColumnCount() {
		return gridColumnCount;
	}

	@ValueRange(minValue=2,maxValue=500)
	public void setGridColumnCount(int gridColumnCount) {
		this.gridColumnCount = gridColumnCount;
	}

	public int getGridRowCount() {
		return gridRowCount;
	}

    @ValueRange(minValue=2,maxValue=500)
	public void setGridRowCount(int gridRowCount) {
		this.gridRowCount = gridRowCount;
	}

	public float getMaxParticleSpeed() {
		return maxParticleSpeed;
	}

    @ValueRange(minValue=0,maxValue=50)
	public void setMaxParticleSpeed(float maxParticleSpeed) {
		this.maxParticleSpeed = maxParticleSpeed;
	}

	public int getForkJoinBatchSize() {
		return forkJoinBatchSize;
	}

    @ValueRange(minValue=0,maxValue=2000)
	public void setForkJoinBatchSize(int forkJoinBatchSize) {
		this.forkJoinBatchSize = forkJoinBatchSize;
	}

	public float getSpringDampening() {
		return springDampening;
	}

    @ValueRange(minValue=0,maxValue=10)
	public void setSpringDampening(float springDampening) {
		this.springDampening = springDampening;
	}

	public float getSpringCoefficient() {
		return springCoefficient;
	}

    @ValueRange(minValue=0,maxValue=1)
	public void setSpringCoefficient(float springCoefficient) {
		this.springCoefficient = springCoefficient;
	}

	public float getParticleMass() {
		return particleMass;
	}

    @ValueRange(minValue=0,maxValue=10)
	public void setParticleMass(float particleMass) {
		this.particleMass = particleMass;
	}

	public boolean isDebugPerformance() {
		return debugPerformance;
	}

	public void setDebugPerformance(boolean debugPerformance) {
		this.debugPerformance = debugPerformance;
	}

    public int getIterationCount()
    {
        return iterationCount;
    }
    
    @ValueRange(minValue=1,maxValue=30)
    public void setIterationCount(int iterationCount)
    {
        this.iterationCount = iterationCount;
    }
    
    public boolean isWaitForVSync()
    {
        return waitForVSync;
    }
    
    public void setWaitForVSync(boolean waitForVSync)
    {
        this.waitForVSync = waitForVSync;
    }
    
    public float getMinWindForce() {
    	return windParameters.getMinForce();
    }
    
    @ValueRange(minValue=0,maxValue=100)
    @Label("Wind: Min. force")
    public void setMinWindForce(float value) {
    	windParameters.setMinForce(value);
    } 
    
    public float getMaxWindForce() {
    	return windParameters.getMaxForce();
    }
    
    @ValueRange(minValue=0,maxValue=100)
    @Label("Wind: Max. force")    
    public void setMaxWindForce(float value) {
    	windParameters.setMaxForce(value);
    }     
    
    public boolean getWindEnabled() {
    	return windParameters.isEnabled();
    }
    
    @Label("Wind: enabled?")
    public void setWindEnabled(boolean yesNo) {
    	windParameters.setEnabled( yesNo );
    }
    
	public int getWindStepsUntilDirectionAdjusted() {
		return this.windParameters.getStepsUntilDirectionAdjusted();
	}
	
	@ValueRange(minValue=1,maxValue=10000)
	@Label("Wind: steps until adjustment")
	public void setWindStepsUntilDirectionAdjusted(int steps) {
		this.windParameters.setStepsUntilDirectionAdjusted(steps);
	}	
	
	public int getWindStepsUntilDirectionChanged() {
		return this.windParameters.getStepsUntilDirectionChanged();
	}
	
	@ValueRange(minValue=1,maxValue=10000)
	@Label("Wind: steps until wind change")
	public void setWindStepsUntilDirectionChanged(int steps) {
		this.windParameters.setStepsUntilDirectionChanged(steps);
	}	
	
	public void setMaxXYAngleInRad(float maxXYAngleInRad) {
		this.windParameters.getMaxAngle().setXYAngleInRad( maxXYAngleInRad );
	}
	
	public float getMaxXYAngleInRad() {
		return this.windParameters.getMaxAngle().getXYAngleInRad();
	}
	
	public void setMaxXZAngleInRad(float maxXZAngleInRad) {
		windParameters.getMaxAngle().setXZAngleInRad( maxXZAngleInRad );
	}
	
	public float getMaxXZAngleInRad() {
		return windParameters.getMaxAngle().getXZAngleInRad();
	}
	
	public void setMinXYAngleInRad(float minXYAngleInRad) {
		this.windParameters.getMinAngle().setXYAngleInRad( minXYAngleInRad );
	}
	
	public float getMinXYAngleInRad() {
		return this.windParameters.getMinAngle().getXYAngleInRad();
	}
	
	public void setMinXZAngleInRad(float minXZAngleInRad) {
		this.windParameters.getMinAngle().setXZAngleInRad( minXZAngleInRad );
	}
	
	public float getMinXZAngleInRad() {
		return this.windParameters.getMinAngle().getXZAngleInRad();
	}	
}