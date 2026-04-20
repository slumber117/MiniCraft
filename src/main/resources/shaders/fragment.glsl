#version 330 core

in vec2 outTexCoord;
in vec2 vLightLevel;
in vec3 vPosition;

layout (location = 0) out vec4 outAlbedo;
layout (location = 1) out vec4 outNormal; // FIXED: was vec3, must match RGBA8 attachment

uniform sampler2D texture_sampler;
uniform vec4 colorTint;
uniform float useLighting;
uniform float sunBrightness;
uniform int weatherType;

void main() {
    vec4 texColor = texture(texture_sampler, outTexCoord) * colorTint;
    
    // Alpha discard for transparency
    if (texColor.a < 0.1) discard;

    // Normal mapping via screen-space derivatives
    vec3 rawNormal = normalize(cross(dFdx(vPosition), dFdy(vPosition)));
    vec4 storedNormal = vec4(rawNormal * 0.5 + 0.5, 1.0);

    if (useLighting < 0.5) {
        outAlbedo = texColor;
        outNormal = storedNormal;
        return;
    }

    outAlbedo = vec4(texColor.rgb, texColor.a);
    outNormal = storedNormal;
}