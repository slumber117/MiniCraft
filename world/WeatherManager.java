package minicraft.world;

/**
 * Manages global weather patterns.
 */
public class WeatherManager {

    public enum WeatherType {
        CLEAR, RAIN, SNOW, THUNDERSTORM, BLIZZARD, HURRICANE, CYCLONE, TORRENTIAL_RAIN
    }

    private WeatherType currentType = WeatherType.CLEAR;
    private float weatherTime = 0;
    private float nextChange = 12000; // Change every ~2 mins (60fps * 120s)

    public void update(float dt) {
        weatherTime += dt * 60f; // Simplified ticks
        if (weatherTime >= nextChange) {
            weatherTime = 0;
            double r = Math.random();
            
            if (r < 0.50)      currentType = WeatherType.CLEAR;
            else if (r < 0.65) currentType = WeatherType.RAIN;
            else if (r < 0.75) currentType = WeatherType.SNOW;
            else if (r < 0.82) currentType = WeatherType.THUNDERSTORM;
            else if (r < 0.88) currentType = WeatherType.TORRENTIAL_RAIN;
            else if (r < 0.93) currentType = WeatherType.BLIZZARD;
            else if (r < 0.97) currentType = WeatherType.CYCLONE;
            else               currentType = WeatherType.HURRICANE;

            nextChange = 6000 + (float) Math.random() * 15000;
            System.out.println("Weather changed to: " + currentType);
        }
    }

    public WeatherType getCurrentType() {
        return currentType;
    }

    public float getIntensity() {
        switch (currentType) {
            case CLEAR:           return 0f;
            case RAIN:            return 0.3f;
            case SNOW:            return 0.3f;
            case THUNDERSTORM:    return 0.6f;
            case TORRENTIAL_RAIN: return 0.8f;
            case BLIZZARD:        return 0.8f;
            case CYCLONE:         return 1.0f;
            case HURRICANE:       return 1.2f; // Overdrive!
            default:              return 0f;
        }
    }

    public float getRainIntensity() {
        if (currentType == WeatherType.SNOW || currentType == WeatherType.BLIZZARD || currentType == WeatherType.CLEAR)
            return 0f;
        return getIntensity();
    }

    public float getSunBrightness() {
        // Base day brightness is 1.0. Intensity dims it.
        return Math.max(0.2f, 1.0f - getIntensity() * 0.5f);
    }
}
