#version 430 core

layout (local_size_x = 16, local_size_y = 16) in;

// Image unit stays at 0 — write target
layout (binding = 0, rgba16f) uniform image2D imgOutput;

// Samplers moved to 1, 2, 3 — no longer conflict with imgOutput
layout (binding = 1) uniform sampler2D texAlbedo;
layout (binding = 2) uniform sampler2D texNormal;
layout (binding = 3) uniform sampler2D texDepth;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 invProjection;
uniform mat4 invView;

uniform vec3 cameraPos;
uniform float uTime;

float random(vec3 co) {
    return fract(sin(dot(co, vec3(12.9898, 78.233, 45.164))) * 43758.5453);
}

vec3 getPosition(vec2 uv) {
    float depth = texture(texDepth, uv).r;
    vec4 clipSpace = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 viewSpace = invProjection * clipSpace;
    viewSpace /= viewSpace.w;
    return (invView * viewSpace).xyz;
}

void main() {
    ivec2 pixelCoords = ivec2(gl_GlobalInvocationID.xy);
    ivec2 dims = imageSize(imgOutput);
    if (pixelCoords.x >= dims.x || pixelCoords.y >= dims.y) return;

    vec2 uv = vec2(pixelCoords) / vec2(dims);

    vec4 albedoData = texture(texAlbedo, uv);
    vec3 normal = texture(texNormal, uv).rgb * 2.0 - 1.0;
    float depth = texture(texDepth, uv).r;

    if (depth >= 1.0) {
        imageStore(imgOutput, pixelCoords, vec4(0, 0, 0, 1));
        return;
    }

    vec3 worldPos = getPosition(uv);

    vec3 indirectLight = vec3(0.0);
    int rayCount = 4; // Increased from 2 for better signal-to-noise ratio
    float seed = uTime + random(vec3(uv, uTime));

    for (int i = 0; i < rayCount; i++) {
        float phi = 6.2831 * random(vec3(uv, seed + float(i)));
        float cosTheta = random(vec3(uv, seed - float(i)));
        float sinTheta = sqrt(max(0.0, 1.0 - cosTheta * cosTheta));

        vec3 rayOrg = worldPos + normal * 0.15;

        vec3 up = abs(normal.z) < 0.999 ? vec3(0, 0, 1) : vec3(1, 0, 0);
        vec3 tangent = normalize(cross(up, normal));
        vec3 bitangent = cross(normal, tangent);

        vec3 rayDir = tangent * cos(phi) * sinTheta
                    + bitangent * sin(phi) * sinTheta
                    + normal * cosTheta;

        vec3 currPos = rayOrg;
        float stepSize = 0.4;

        for (int j = 0; j < 18; j++) {
            currPos += rayDir * stepSize;

            vec4 proj = projectionMatrix * viewMatrix * vec4(currPos, 1.0);
            if (proj.w <= 0.0) break;
            proj.xyz /= proj.w;
            vec2 sampleUV = proj.xy * 0.5 + 0.5;

            if (sampleUV.x < 0.0 || sampleUV.x > 1.0 ||
                sampleUV.y < 0.0 || sampleUV.y > 1.0) break;

            float zSample = texture(texDepth, sampleUV).r;
            if (zSample >= 1.0) continue;

            vec3 sampledWorldPos = getPosition(sampleUV);
            float distToSurface = distance(rayOrg, sampledWorldPos);
            float distToRay = distance(rayOrg, currPos);

            if (distToRay > distToSurface) {
                // FIXED: tightened from stepSize * 2.5 to stepSize * 1.2
                if (abs(distToRay - distToSurface) < stepSize * 1.2) {
                    vec3 hitAlbedo = texture(texAlbedo, sampleUV).rgb;
                    float atten = 1.0 / (1.0 + distToRay * distToRay);
                    indirectLight += hitAlbedo * atten;
                    break;
                }
            }
        }
    }

    indirectLight /= float(rayCount);
    indirectLight = clamp(indirectLight, 0.0, 2.0);

    imageStore(imgOutput, pixelCoords, vec4(indirectLight, 1.0));
}