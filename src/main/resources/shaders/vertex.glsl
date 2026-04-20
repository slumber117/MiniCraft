#version 330 core

layout (location=0) in vec3 position;
layout (location=1) in vec2 texCoord;
layout (location=2) in vec2 lightInfo;

out vec2 outTexCoord;
out vec2 vLightLevel;
out vec3 vPosition;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 modelMatrix;
uniform float uTime;

void main()
{
    vec3 animatedPos = position;
    
    // Smooth Torch Flicker (Move top vertices only)
    if (position.y > 0.1 && position.y < 0.9) {
        float wobble = sin(uTime * 10.0 + position.y * 5.0) * 0.05;
        animatedPos.x += wobble;
        animatedPos.z += sin(uTime * 8.0) * 0.05;
    }

    vec4 worldPos = modelMatrix * vec4(animatedPos, 1.0);
    gl_Position = projectionMatrix * viewMatrix * worldPos;
    outTexCoord = texCoord;
    vLightLevel = vec2(1.0, 1.0); // DIAGNOSTIC: Bypass zeroed voxel lighting
    vPosition   = worldPos.xyz;
}
