in vec3 a_position;
in vec2 a_texCoords0;
in mat4 i_worldTrans;

uniform mat4 u_projViewTrans;
out vec2 TexCoords;

void main () {
    TexCoords = a_texCoords0;
    gl_Position = u_projViewTrans * i_worldTrans * vec4(a_position, 1.0);
}
