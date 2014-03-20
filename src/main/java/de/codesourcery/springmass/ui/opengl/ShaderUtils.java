package de.codesourcery.springmass.ui.opengl;

import java.io.*;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class ShaderUtils {

	public static ShaderProgram loadShader(String name) throws IOException {
		
		final String vShaderClasspathPath= "/vertexshader/"+name+"_vertex.glsl";
		final String fShaderClasspathPath= "/fragmentshader/"+name+"_fragment.glsl";
		
		final String vshader = loadFromClasspath( vShaderClasspathPath );
		final String fshader = loadFromClasspath( fShaderClasspathPath );
		
		System.out.println("==== vertex shader ====\n\n"+vshader);
		System.out.println("\n\n==== fragment shader ====\n\n"+fshader);
		final ShaderProgram result =  new ShaderProgram( vshader , fshader );
		if ( ! result.isCompiled() ) 
		{
			System.err.println("Shader compilation failed: "+result.getLog());
			throw new RuntimeException("Shader compilation failed");
		}
		return result;
	}
	
	private static String loadFromClasspath(String path) throws IOException {
		
		final InputStream in = ShaderUtils.class.getResourceAsStream( path );
		if ( in == null ) {
			throw new RuntimeException("Failed to load shader from classpath "+path);
		}
		final BufferedReader reader = new BufferedReader( new InputStreamReader(in) );
		try {
		StringBuilder builder = new StringBuilder();
		String line;
		
		while ( ( line = reader.readLine()) != null ) {
			builder.append( line+"\n" );
		}
		return builder.toString();
		} finally {
			reader.close();
		}
	}
	
}
