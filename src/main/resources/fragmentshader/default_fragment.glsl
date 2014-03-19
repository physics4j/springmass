// uniform sampler2D color_texture;
    
in vec3 rotatedSurfaceNormal;
in vec3 v_lightDir;

smooth out vec4 vFragColor;

void main()                                   
{
   const vec3 ambientColor = vec3(0,0,0.3);
   
   // phong shading
   float diff = max(0.0 , abs( dot( normalize( rotatedSurfaceNormal ) , normalize( v_lightDir ) ) ) );

   // diffuse
   vFragColor.a = 1.0;
   vFragColor.rgb = diff*vec3(0,0,0.9);
      
   // ambient
   vFragColor.rgb += ambientColor;
   
   // specular
   // vec3 vReflection = normalize( reflect( -normalize(v_lightDir),normalize( rotatedSurfaceNormal ) ) );
   
   // float spec = max(0.0,dot(normalize( rotatedSurfaceNormal ) , vReflection ));
   
   // if ( diff != 0 ) {
      // float fSpec = pow(spec,128.0);
      // vFragColor.rgb += vec3(fSpec,fSpec,fSpec);    
   // }
}