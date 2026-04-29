#version 430 core

in vec2 outTexCoord;
out vec4 fragColor;

layout (binding = 0) uniform sampler2D texAlbedo;
layout (binding = 1) uniform sampler2D texGI;
layout (binding = 2) uniform sampler2D texNormal;
layout (binding = 3) uniform sampler2D texDepth;
uniform int rtgiEnabled;

uniform vec3 torchPos;
uniform float torchStrength;
uniform vec3 torchColor;

uniform mat4 invProjection;
uniform mat4 invView;

// Reconstruct world position from depth
vec3 getPosition(vec2 uv) {
    float depth = texture(texDepth, uv).r;
    vec4 clipSpace = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 viewSpace = invProjection * clipSpace;
    viewSpace /= viewSpace.w;
    return (invView * viewSpace).xyz;
}

// ACES Filmic Tonemapping
vec3 ACESFilm(vec3 x) {
    float a = 2.51;
    float b = 0.03;
    float c = 2.43;
    float d = 0.59;
    float e = 0.14;
    return clamp((x*(a*x+b))/(x*(c*x+d)+e), 0.0, 1.0);
}

void main() {
    vec2 uv = outTexCoord;
    vec3 albedo = texture(texAlbedo, uv).rgb;

    // Ambient lighting (Default base)
    float ambient = 0.4;
    vec3 result = albedo * ambient;

    // Advanced Bilateral Denoising
    if (rtgiEnabled > 0) {
        vec3 centerGI = texture(texGI, uv).rgb;
        vec3 centerNormal = texture(texNormal, uv).rgb * 2.0 - 1.0;
        float centerDepth = texture(texDepth, uv).r;

        vec3 sumGI = vec3(0.0);
        float sumWeight = 0.0;
        
        vec2 texelSize = 1.0 / textureSize(texGI, 0);
        const int radius = 3; // 7x7 kernel total

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                vec2 offsetUV = uv + vec2(x, y) * texelSize;
                vec3 sampleGI = texture(texGI, offsetUV).rgb;
                vec3 sampleNormal = texture(texNormal, offsetUV).rgb * 2.0 - 1.0;
                float sampleDepth = texture(texDepth, offsetUV).r;

                // Bilateral Weights
                float distWeight = exp(-(float(x*x + y*y)) / 8.0);
                float normalWeight = pow(max(0.0, dot(centerNormal, sampleNormal)), 32.0);
                float depthWeight = exp(-abs(centerDepth - sampleDepth) * 500.0);

                float weight = distWeight * normalWeight * depthWeight;
                sumGI += sampleGI * weight;
                sumWeight += weight;
            }
        }
        
        vec3 finalGI = (sumWeight > 0.0001) ? (sumGI / sumWeight) : centerGI;

        // Composite with a slightly stronger multiplier since noise is suppressed
        result += albedo * finalGI * 1.2;
    }

    // --- Restore Torchlight (Point Light) ---
    if (torchStrength > 0.01) {
        vec3 worldPos = getPosition(uv);
        vec3 normal = texture(texNormal, uv).rgb * 2.0 - 1.0;
        vec3 toLight = torchPos - worldPos;
        float dist = length(toLight);
        
        if (dist < 20.0) { // Limit effect range
            vec3 L = normalize(toLight);
            float diff = max(0.0, dot(normal, L));
            float atten = torchStrength / (1.0 + 0.1 * dist + 0.04 * dist * dist);
            
            // Apply torch color tint
            result += albedo * diff * atten * torchColor;
        }
    }

    // High-fidelity post-processing
    result = ACESFilm(result);
    result = pow(result, vec3(1.0 / 2.2)); // Gamma correction

    fragColor = vec4(result, 1.0);
}
