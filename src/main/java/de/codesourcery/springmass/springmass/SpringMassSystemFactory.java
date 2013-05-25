package de.codesourcery.springmass.springmass;

import java.awt.Color;

import de.codesourcery.springmass.math.Vector4;

public class SpringMassSystemFactory {

	public SpringMassSystem create(SimulationParameters parameters) 
	{
		System.out.println("Point masses: "+(parameters.getGridRows()*parameters.getGridColumns()));
		
		final Mass[][] masses = new Mass[parameters.getGridColumns()][];
		for ( int i = 0 ; i < parameters.getGridColumns() ; i++ ) {
			masses[i] = new Mass[parameters.getGridRows()];
		}

		double scaleX = parameters.getXResolution() / (parameters.getGridColumns()+parameters.getGridColumns()*0.5);
		double scaleY = parameters.getYResolution() / (parameters.getGridRows()+parameters.getGridRows()*0.5);
		
		final double xOffset = scaleX;
		final double yOffset = scaleY;
		
		for ( int x = 0 ; x < parameters.getGridColumns() ; x++ ) 
		{
			for ( int y = 0 ; y < parameters.getGridRows() ; y++ ) 
			{
				final Vector4 pos = new Vector4( xOffset + scaleX*x , yOffset + scaleY*y,0);
				final Mass m = new Mass( Color.red  , pos );
				if ( y == 0 ) {
					m.setFixed( true );
				}
				masses[x][y] = m;
			}
		}
		
		final SpringMassSystem system = new SpringMassSystem(parameters,masses);

		// connect masses horizontally
		final double horizRestLength = scaleX*parameters.getHorizontalRestLengthFactor();
		for ( int y = 0 ; y < parameters.getGridRows() ; y++ ) 
		{
			for ( int x = 0 ; x < (parameters.getGridColumns()-1) ; x++ ) 
			{
				system.addSpring( new Spring( masses[x][y] , masses[x+1][y] , horizRestLength , true ) );
			}
		}

		// connect masses vertically
		final double verticalRestLength = scaleY*parameters.getVerticalRestLengthFactor();
		for ( int x = 0 ; x < parameters.getGridColumns() ; x++ ) 
		{
			for ( int y = 0 ; y < (parameters.getGridRows()-1) ; y++ ) 
			{
				system.addSpring( new Spring( masses[x][y] , masses[x][y+1] , verticalRestLength , true ) );
			}
		}	

		// cross-connect masses
		final double crossConnectRestLength = Math.sqrt( horizRestLength*horizRestLength + verticalRestLength*verticalRestLength);
		for ( int x = 0 ; x < (parameters.getGridColumns()-1) ; x++ ) 
		{
			for ( int y = 0 ; y < (parameters.getGridRows()-1) ; y++ ) 
			{
				system.addSpring( new Spring( masses[x][y] , masses[x+1][y+1] , crossConnectRestLength , parameters.isRenderAllLines() , Color.YELLOW ) );
				system.addSpring( new Spring( masses[x][y+1] , masses[x+1][y] , crossConnectRestLength , parameters.isRenderAllLines() , Color.YELLOW ) );				
			}
		}	

		// connect cloth outline
		final double horizOutlineRestLength = 2 * horizRestLength;
		for ( int y = 0 ; y < parameters.getGridRows() ; y++ ) {
			for ( int x = 0 ; x < (parameters.getGridColumns()-2) ; x++ ) 
			{
				system.addSpring( new Spring( masses[x][y] , masses[x+2][y] , horizOutlineRestLength , parameters.isRenderAllLines(), Color.BLUE ) );
			}	
		}

		final double verticalOutlineRestLength = 2 * verticalRestLength;
		for ( int x = 0 ; x < parameters.getGridColumns() ; x++ ) 
		{ 
			for ( int y = 0 ; y < (parameters.getGridRows()-2) ; y++ ) 
			{
				system.addSpring( new Spring( masses[x][y] , masses[x][y+2] , verticalOutlineRestLength , parameters.isRenderAllLines(), Color.BLUE ) );
			}		
		}
		return system;
	}
}
