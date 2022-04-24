in vec2 texCoords;

out vec4 color;

uniform vec3 fcolor;

uniform sampler2D texSampler;

void main() {
    color.xyz = fcolor;
    color.w = texture(texSampler, texCoords).x;
}