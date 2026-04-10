package minicraft.renderer;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds all loaded block textures, keyed by block texture name.
 * Lazy-loads textures from resources/textures/ on first request.
 */
public class TextureRegistry {

    private final Map<String, Texture> textures = new HashMap<>();
    private final String basePath;

    public TextureRegistry(String basePath) {
        this.basePath = basePath; // e.g. "src/main/resources/textures/"
    }

    public Texture get(String name) {
        if (name == null || name.isEmpty()) return null;
        
        Texture existing = textures.get(name);
        if (existing != null) return existing;

        try {
            Texture t = new Texture(basePath + name + ".png");
            textures.put(name, t);
            return t;
        } catch (Exception e) {
            // ONLY fallback to stone for actual asset names that failed to load.
            // Ignore small/internal names or empty names to prevent "Stone in the Air".
            if (!name.equals("stone") && name.length() > 2) {
                System.err.println("Failed to load texture: " + name + " - " + e.getMessage() + ". Falling back to stone.");
                Texture stone = get("stone");
                if (stone != null) {
                    textures.put(name, stone); 
                }
                return stone;
            }
            return null;
        }
    }

    public void cleanup() {
        textures.values().forEach(t -> { if (t != null) t.cleanup(); });
        textures.clear();
    }
}
