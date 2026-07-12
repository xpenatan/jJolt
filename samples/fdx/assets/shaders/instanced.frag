precision mediump float;

out vec4 FragColor;
uniform sampler2D u_texture;
in vec2 TexCoords;

void main () {
    FragColor = texture(u_texture, TexCoords);
}
