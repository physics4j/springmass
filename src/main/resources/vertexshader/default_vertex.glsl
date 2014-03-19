#version 330

// per-vertex attributes
in vec4 a_position;
in vec4 a_normal;
      
uniform mat4 u_modelView;
uniform mat4 u_modelViewProjection;
uniform mat3 u_cameraRotation;
uniform vec3 vLightPos;

// shader output

out vec3 rotatedSurfaceNormal;
out vec3 v_lightDir;

// smooth out vec2 vTexCoord;

void main()                   
{   
   // apply camera rotation to vertex normal
   rotatedSurfaceNormal = (u_modelView * vec4(a_normal.xyz,0)).xyz;
   
   // transform vertex to eye coordinates
   vec4 eyeVertex = u_modelView * a_position;
   vec3 eyeVertexNormalized = eyeVertex.xyz / eyeVertex.w;
   
   const vec3 lightPos = (u_modelView * vec4(vLightPos,1)).xyz;
      
   // normal vector to light source
   v_lightDir = normalize(lightPos - eyeVertex);
   
   gl_Position =  u_modelViewProjection * a_position;
}