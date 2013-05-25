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

import de.codesourcery.springmass.math.Vector4;

public class SimulationParamsBuilder {

	private int xResolution = 1000;
	private int yResolution = 1000;

	private int frameSleepTime = 20;
	
	private double springDampening=0.1;
	private double springCoefficient=0.1;	

	private boolean renderAllLines = false;

	private boolean renderSprings = false;

	private boolean renderMasses = false;
	
	private double particleMass = 1.0;

	private double mouseDragZDepth = -100;

	private double verticalRestLengthFactor = 1;
	private double horizontalRestLengthFactor = 1;

	private boolean lightSurfaces = true;
	
	private Vector4 lightPosition = new Vector4(xResolution / 3.5, yResolution / 2.5, -200);
	private Color lightColor = new Color(50, 50, 200);

	private double gravity = 9.81;

	private int gridColumnCount = 33;
	private int gridRowCount = 33;

	private double maxParticleSpeed = 20;
	private int forkJoinBatchSize = 1000;
	
	/**
	 * Method that is never considered a parameter getter/setter.
	 *
	 * @author tobias.gierke@voipfuture.com
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface IgnoreMethod { }
	
	public abstract class SimulationParameter 
	{
		public abstract String getName();
		
		public abstract Class<?> getType();
		
		public abstract Object getValue();
		
		public abstract boolean isReadOnly();
		
		public abstract boolean isWriteOnly();
		
		public abstract void setValue(Object value);
		
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
	
	public static void main(String[] args) {
		final List<SimulationParameter> parameters = new SimulationParamsBuilder().getParameters();
		System.out.println( StringUtils.join( parameters , "\n" ) );
	}

	public SimulationParameters build() 
	{
		return new SimulationParameters(xResolution, yResolution, 
				frameSleepTime, renderAllLines, renderSprings, renderMasses, mouseDragZDepth, 
				verticalRestLengthFactor, horizontalRestLengthFactor, 
				lightSurfaces, lightPosition, lightColor, gravity, 
				gridColumnCount, gridRowCount, maxParticleSpeed, forkJoinBatchSize,springCoefficient , springDampening,particleMass);
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
	
	private String extractParameterName(Method m) 
	{
		final String name;
		if ( m.getName().startsWith("set") ||  m.getName().startsWith("get")) {
			name = m.getName().substring(3);
		} 
		else if ( m.getName().startsWith("is" ) ) 
		{
			name = m.getName().substring(2);
		} else {
			throw new IllegalArgumentException("Invalid method name: "+m); 
		}
		return Character.toUpperCase( name.charAt(0) )+name.substring(1);
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
		lightPosition = new Vector4(xResolution / 3.5, yResolution / 2.5, -200);		
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

	public int getFrameSleepTime() {
		return frameSleepTime;
	}

	public void setFrameSleepTime(int frameSleepTime) {
		this.frameSleepTime = frameSleepTime;
	}

	public boolean isRenderAllLines() {
		return renderAllLines;
	}

	public void setRenderAllLines(boolean renderAllLines) {
		this.renderAllLines = renderAllLines;
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

	public double getMouseDragZDepth() {
		return mouseDragZDepth;
	}

	public void setMouseDragZDepth(double mouseDragZDepth) {
		this.mouseDragZDepth = mouseDragZDepth;
	}

	public double getVerticalRestLengthFactor() {
		return verticalRestLengthFactor;
	}

	public void setVerticalRestLengthFactor(double verticalRestLengthFactor) {
		this.verticalRestLengthFactor = verticalRestLengthFactor;
	}

	public double getHorizontalRestLengthFactor() {
		return horizontalRestLengthFactor;
	}

	public void setHorizontalRestLengthFactor(double horizontalRestLengthFactor) {
		this.horizontalRestLengthFactor = horizontalRestLengthFactor;
	}

	public boolean isLightSurfaces() {
		return lightSurfaces;
	}

	public void setLightSurfaces(boolean lightSurfaces) {
		this.lightSurfaces = lightSurfaces;
	}

	public Vector4 getLightPosition() {
		return lightPosition;
	}

	public void setLightPosition(Vector4 lightPosition) {
		this.lightPosition = lightPosition;
	}

	public Color getLightColor() {
		return lightColor;
	}

	public void setLightColor(Color lightColor) {
		this.lightColor = lightColor;
	}

	public double getGravity() {
		return gravity;
	}

	public void setGravity(double gravity) {
		this.gravity = gravity;
	}

	public int getGridColumnCount() {
		return gridColumnCount;
	}

	public void setGridColumnCount(int gridColumnCount) {
		this.gridColumnCount = gridColumnCount;
	}

	public int getGridRowCount() {
		return gridRowCount;
	}

	public void setGridRowCount(int gridRowCount) {
		this.gridRowCount = gridRowCount;
	}

	public double getMaxParticleSpeed() {
		return maxParticleSpeed;
	}

	public void setMaxParticleSpeed(double maxParticleSpeed) {
		this.maxParticleSpeed = maxParticleSpeed;
	}

	public int getForkJoinBatchSize() {
		return forkJoinBatchSize;
	}

	public void setForkJoinBatchSize(int forkJoinBatchSize) {
		this.forkJoinBatchSize = forkJoinBatchSize;
	}

	public double getSpringDampening() {
		return springDampening;
	}

	public void setSpringDampening(double springDampening) {
		this.springDampening = springDampening;
	}

	public double getSpringCoefficient() {
		return springCoefficient;
	}

	public void setSpringCoefficient(double springCoefficient) {
		this.springCoefficient = springCoefficient;
	}

	public double getParticleMass() {
		return particleMass;
	}

	public void setParticleMass(double particleMass) {
		this.particleMass = particleMass;
	}
}