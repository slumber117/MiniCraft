#version 430 core

// ════════════════════════════════════════════════════════════════════════
// FSR 1.0 — Robust Contrast-Adaptive Sharpening (RCAS)
// Based on AMD FidelityFX Super Resolution 1.0
// Ported to GLSL compute shader for OpenGL 4.3
// ════════════════════════════════════════════════════════════════════════

layout (local_size_x = 16, local_size_y = 16) in;

layout (binding = 0, rgba8) uniform image2D imgOutput;

uniform float sharpness; // 0.0 = maximum sharpness, 1.0 = no sharpening

void main() {
    ivec2 coord = ivec2(gl_GlobalInvocationID.xy);
    ivec2 dims = imageSize(imgOutput);
    if (coord.x >= dims.x || coord.y >= dims.y) return;

    // Load cross-shaped neighborhood
    vec3 center = imageLoad(imgOutput, coord).rgb;
    vec3 north  = imageLoad(imgOutput, clamp(coord + ivec2( 0, -1), ivec2(0), dims - 1)).rgb;
    vec3 south  = imageLoad(imgOutput, clamp(coord + ivec2( 0,  1), ivec2(0), dims - 1)).rgb;
    vec3 west   = imageLoad(imgOutput, clamp(coord + ivec2(-1,  0), ivec2(0), dims - 1)).rgb;
    vec3 east   = imageLoad(imgOutput, clamp(coord + ivec2( 1,  0), ivec2(0), dims - 1)).rgb;

    // Luma for contrast detection
    float lumC = dot(center, vec3(0.299, 0.587, 0.114));
    float lumN = dot(north,  vec3(0.299, 0.587, 0.114));
    float lumS = dot(south,  vec3(0.299, 0.587, 0.114));
    float lumW = dot(west,   vec3(0.299, 0.587, 0.114));
    float lumE = dot(east,   vec3(0.299, 0.587, 0.114));

    // Min/Max of neighbors for contrast calculation
    float minLum = min(min(lumN, lumS), min(lumW, lumE));
    float maxLum = max(max(lumN, lumS), max(lumW, lumE));

    // Local contrast ratio
    float contrast = maxLum - minLum;
    float range = max(maxLum, 1e-5);

    // Adaptive sharpening weight — less sharpening in high-contrast (noisy) areas
    // This prevents halo artifacts around hard edges
    float sharpenStrength = sqrt(min(minLum, 1.0 - maxLum) / range);
    sharpenStrength = clamp(sharpenStrength, 0.0, 1.0);

    // Scale by user sharpness control (0 = max sharp, 1 = off)
    float w = sharpenStrength * (1.0 - sharpness) * -0.25;

    // Apply sharpening filter: center + w * (neighbors - 4*center)
    // Rearranged: center * (1 - 4w) + w * (N + S + W + E)
    float totalWeight = 1.0 + 4.0 * w;
    vec3 result = (center + w * (north + south + west + east)) / totalWeight;

    // Clamp to valid range
    result = clamp(result, 0.0, 1.0);

    imageStore(imgOutput, coord, vec4(result, 1.0));
}
