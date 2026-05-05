#version 430 core

// ════════════════════════════════════════════════════════════════════════
// FSR 1.0 — Edge-Adaptive Spatial Upsampling (EASU)
// Based on AMD FidelityFX Super Resolution 1.0
// Ported to GLSL compute shader for OpenGL 4.3
// ════════════════════════════════════════════════════════════════════════

layout (local_size_x = 16, local_size_y = 16) in;

layout (binding = 0, rgba8) uniform image2D imgOutput;  // Display resolution
layout (binding = 1) uniform sampler2D texInput;         // Render resolution

uniform vec2 inputSize;   // Render resolution (e.g. 960x540)
uniform vec2 outputSize;  // Display resolution (e.g. 1280x720)

// Lanczos2 approximation weight
float lanczos2(float x) {
    float a = max(abs(x), 1e-5);
    if (a > 2.0) return 0.0;
    float pi_a = 3.14159265 * a;
    float pi_a_half = pi_a * 0.5;
    return (sin(pi_a) / pi_a) * (sin(pi_a_half) / pi_a_half);
}

void main() {
    ivec2 outCoord = ivec2(gl_GlobalInvocationID.xy);
    ivec2 outDims = ivec2(outputSize);
    if (outCoord.x >= outDims.x || outCoord.y >= outDims.y) return;

    // Map output pixel to input space
    vec2 uv = (vec2(outCoord) + 0.5) / outputSize;
    vec2 srcPos = uv * inputSize - 0.5;
    vec2 srcFloor = floor(srcPos);
    vec2 frac = srcPos - srcFloor;

    // ── Edge Detection ──
    // Sample a 4x4 neighborhood for edge-aware filtering
    vec3 colorSum = vec3(0.0);
    float weightSum = 0.0;

    // Compute gradient to detect edges
    vec3 sampleC = texture(texInput, (srcFloor + 0.5 + frac) / inputSize).rgb;
    vec3 sampleL = texture(texInput, (srcFloor + vec2(-0.5, 0.5) + frac) / inputSize).rgb;
    vec3 sampleR = texture(texInput, (srcFloor + vec2(1.5, 0.5) + frac) / inputSize).rgb;
    vec3 sampleU = texture(texInput, (srcFloor + vec2(0.5, -0.5) + frac) / inputSize).rgb;
    vec3 sampleD = texture(texInput, (srcFloor + vec2(0.5, 1.5) + frac) / inputSize).rgb;

    // Luminance-based edge detection
    float lumC = dot(sampleC, vec3(0.299, 0.587, 0.114));
    float lumL = dot(sampleL, vec3(0.299, 0.587, 0.114));
    float lumR = dot(sampleR, vec3(0.299, 0.587, 0.114));
    float lumU = dot(sampleU, vec3(0.299, 0.587, 0.114));
    float lumD = dot(sampleD, vec3(0.299, 0.587, 0.114));

    // Sobel-style gradient
    float gradX = abs(lumR - lumL);
    float gradY = abs(lumD - lumU);
    float edgeStrength = clamp(gradX + gradY, 0.0, 1.0);

    // Edge direction (0 = horizontal edge, 1 = vertical edge)
    float edgeDir = gradX / (gradX + gradY + 1e-5);

    // ── Directional Lanczos Filtering ──
    // Sample 12 points in a cross pattern, weighted by edge direction
    for (int j = -1; j <= 2; j++) {
        for (int i = -1; i <= 2; i++) {
            vec2 sampleOffset = vec2(float(i), float(j));
            vec2 delta = sampleOffset - frac;

            // Directional stretch: elongate filter perpendicular to edge
            float dx = delta.x;
            float dy = delta.y;

            // Stretch the filter kernel along the edge
            float edgeScale = 1.0 + edgeStrength * 0.5;
            float crossScale = 1.0 / edgeScale;
            float sx = mix(dx, dx * crossScale, edgeDir);
            float sy = mix(dy * crossScale, dy, edgeDir);

            float dist = sqrt(sx * sx + sy * sy);
            float w = lanczos2(dist);

            if (w > 0.0) {
                vec2 sUV = (srcFloor + sampleOffset + 0.5) / inputSize;
                sUV = clamp(sUV, vec2(0.0), vec2(1.0));
                vec3 s = texture(texInput, sUV).rgb;
                colorSum += s * w;
                weightSum += w;
            }
        }
    }

    vec3 result = (weightSum > 0.0) ? colorSum / weightSum : sampleC;

    // Clamp to prevent ringing artifacts
    vec3 minCol = min(min(sampleL, sampleR), min(sampleU, sampleD));
    vec3 maxCol = max(max(sampleL, sampleR), max(sampleU, sampleD));
    // Allow slight overshoot for sharpness (anti-ringing with tolerance)
    float tolerance = 0.1 * edgeStrength;
    result = clamp(result, minCol - tolerance, maxCol + tolerance);

    imageStore(imgOutput, outCoord, vec4(result, 1.0));
}
