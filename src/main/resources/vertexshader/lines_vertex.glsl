#version 330

in vec4 a_position;
in vec3 a_color;
      
uniform mat4 u_modelViewProjection;

out vec4 color;

void main()                   
{   
   gl_Position =  u_modelViewProjection * a_position;
   color = vec4(a_color,1);
}