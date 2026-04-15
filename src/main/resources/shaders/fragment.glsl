#version 330 core

in vec2 outTexCoord;
in vec2 vLightLevel;
in vec3 vPosition;

out vec4 fragColor;

uniform sampler2D texture_sampler;
uniform vec4 colorTint;
uniform vec3 torchPos;
uniform float torchStrength; // Handheld torch
uniform vec3 glowColor;      // Armor radiation (Uranium/Plutonium)
uniform float glowStrength;  
uniform float sunBrightness;  // 0.05 to 1.0 (Day/Night)
uniform float weatherIntensity; // 0.0 to 1.0+
uniform int   weatherType;      // 0=Clear, 1=Rain, 2=Snow, etc.
uniform float useLighting;   // 1.0 = on, 0.0 = off

void main()
{
    vec4 texColor = texture(texture_sampler, outTexCoord) * colorTint;
    
    // Smart Alpha-Clipping for JPEGs (Discard white backgrounds)
    if (texColor.r > 0.98 && texColor.g > 0.98 && texColor.b > 0.98) discard;
    if (texColor.a < 0.1) discard;

    if (useLighting < 0.5) {
        fragColor = texColor;
        return;
    }

    // Dynamic Torch Light (Handheld)
    float dist = distance(vPosition, torchPos);
    float torchEffect = 0.0;
    if (torchStrength > 0.0) {
        float falloff = 25.0; 
        torchEffect = clamp(1.0 - (dist / falloff), 0.0, 1.0) * torchStrength;
    }

    // Combine Sunlight (vLightLevel.x) with SunBrightness, 
    // and add static Block Light (vLightLevel.y)
    float staticLight = max(vLightLevel.x * sunBrightness, vLightLevel.y);
    
    float finalLight = max(staticLight, torchEffect);
    
    // Apply Tinted Armor Glow (Additive)
    if (glowStrength > 0.0) {
        float glowDist = distance(vPosition, torchPos);
        float glow = clamp(1.0 - (glowDist / 12.0), 0.0, 1.0) * glowStrength; // Smaller radius than torch
        texColor.rgb += glowColor * glow * 0.4;
    }

    finalLight = clamp(finalLight, 0.005, 1.0); 

    vec3 finalColor = texColor.rgb * finalLight;

    // Apply Weather Tint
    if (weatherIntensity > 0.1) {
        vec3 weatherTint = vec3(1.0);
        if (weatherType == 1 || weatherType == 3 || weatherType == 7) // Rain/Storm
            weatherTint = vec3(0.8, 0.8, 0.9);
        else if (weatherType == 2 || weatherType == 5) // Snow/Blizzard
            weatherTint = vec3(0.9, 0.9, 1.0);
            
        finalColor = mix(finalColor, finalColor * weatherTint, weatherIntensity);
    }

    // Apply Simple Fog based on intensity
    float viewDist = length(vPosition - torchPos); // Approx dist from camera
    float fogMin = 50.0 / (1.0 + weatherIntensity * 2.0);
    float fogMax = 150.0 / (1.0 + weatherIntensity * 2.0);
    float fogFactor = clamp((viewDist - fogMin) / (fogMax - fogMin), 0.0, 1.0);
    
    vec3 fogColor = vec3(0.5, 0.5, 0.6) * sunBrightness; // Match sky roughly
    if (weatherType == 2) fogColor = vec3(0.8, 0.8, 0.9) * sunBrightness;

    fragColor = vec4(mix(finalColor, fogColor, fogFactor), texColor.a);
}
