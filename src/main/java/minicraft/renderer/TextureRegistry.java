package minicraft.renderer;

import com.google.gson.JsonObject; // You'll need the GSON library, or use a simple parser
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.io.FileReader;

public class TextureRegistry {
    private final Map<String, TextureRegion> regions = new HashMap<>(); 
    private final Map<String, Texture> standaloneTextures = new HashMap<>(); // Standalone files
    private final String basePath;
    private final com.google.gson.Gson gson = new com.google.gson.Gson();

    public TextureRegistry(String basePath) {
        this.basePath = basePath;
        // Project uses flattened root structure now, no atlases needed
    }

    private final java.util.Set<String> missingTextures = new java.util.HashSet<>();

    public TextureRegion get(String name) {
        if (name == null || name.isEmpty()) return null;

        // 1. Check Cache
        if (regions.containsKey(name)) return regions.get(name);

        // 2. Skip known-missing textures (prevents repeated disk I/O)
        if (missingTextures.contains(name)) {
            TextureRegion fallback = regions.get("grass");
            if (fallback != null) regions.put(name, fallback); // Cache the fallback
            return fallback;
        }

        // 3. Load Individual Texture from Root
        try {
            String fileName = name.endsWith(".png") ? name : name + ".png";
            Texture t = new Texture("/textures/" + fileName);
            TextureRegion region = new TextureRegion(t, 0, 0, 1, 1);
            regions.put(name, region);
            return region;
        } catch (Exception e) {
            missingTextures.add(name);
            System.err.println("Missing texture (will use fallback): " + name);
            if (name.equals("grass")) {
                System.err.println("CRITICAL: Root textures/grass.png not found!");
                return null;
            }
            TextureRegion fallback = get("grass");
            regions.put(name, fallback); // Cache immediately
            return fallback;
        }
    }

    public void cleanup() {
        // Cleanup all parent textures in the regions map
        regions.values().stream().map(TextureRegion::getTexture).distinct().forEach(t -> { if (t != null) t.cleanup(); });
        regions.clear();
        standaloneTextures.clear();
    }
}
