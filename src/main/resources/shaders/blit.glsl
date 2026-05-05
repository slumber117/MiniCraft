#version 330 core

in vec2 outTexCoord;
out vec4 fragColor;

uniform sampler2D texInput;

void main() {
    fragColor = texture(texInput, outTexCoord);
}
