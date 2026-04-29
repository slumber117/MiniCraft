#version 330 core

in vec2 outTexCoord;
layout (location = 0) out vec4 outColor;

uniform sampler2D texture_sampler;
uniform vec4 colorTint;
uniform float useTexture;
uniform float useLighting;

void main() {
    vec4 texColor = texture(texture_sampler, outTexCoord) * colorTint;
    if (texColor.a < 0.05) discard;
    outColor = texColor;
}
