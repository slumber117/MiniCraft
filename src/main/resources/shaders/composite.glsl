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
uniform float playerY;

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

    // ── Depth-Aware Ambient Lighting ──
    // Surface (Y >= 100): full ambient (0.4)
    // Shallow caves (Y 60-100): rapidly fading
    // Deep underground (Y < 60): near-total darkness (0.02)
    float surfaceThreshold = 100.0;
    float caveFloor = 60.0;
    float depthFactor = clamp((playerY - caveFloor) / (surfaceThreshold - caveFloor), 0.0, 1.0);
    float ambient = mix(0.02, 0.4, depthFactor);

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

        // Scale GI contribution by depth — underground GI is dimmer
        float giScale = mix(0.3, 1.2, depthFactor);
        result += albedo * finalGI * giScale;
    }

    // ── Torch Point Light (Tiered Range & Brightness) ──
    if (torchStrength > 0.01) {
        vec3 worldPos = getPosition(uv);
        vec3 normal = texture(texNormal, uv).rgb * 2.0 - 1.0;
        vec3 toLight = torchPos - worldPos;
        float dist = length(toLight);
        
        // Torch range scales with tier: primitive=6, tin=12, gold=20, plutonium=30
        float torchRange = 6.0 + torchStrength * 16.0;
        
        if (dist < torchRange) {
            vec3 L = normalize(toLight);
            float diff = max(0.0, dot(normal, L));
            float atten = torchStrength / (1.0 + 0.08 * dist + 0.03 * dist * dist);
            
            // Apply torch color tint
            result += albedo * diff * atten * torchColor;
        }
    }

    // High-fidelity post-processing
    result = ACESFilm(result);
    result = pow(result, vec3(1.0 / 2.2)); // Gamma correction

    fragColor = vec4(result, 1.0);
}
