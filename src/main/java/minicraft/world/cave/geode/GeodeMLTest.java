package minicraft.world.cave.geode;

public class GeodeMLTest {
    public static void main(String[] args) {
        System.out.println("Starting Geode ML Pipeline Test...");
        
        try {
            GeodeMLPipeline pipeline = new GeodeMLPipeline();
            long seed = 12345L;
            float depth = 0.6f;
            
            System.out.println("Generating blueprint for seed " + seed + "...");
            byte[][][] blueprint = pipeline.generateBlueprint(seed, depth);
            
            int shellCount = 0;
            int crystalCount = 0;
            int hollowCount = 0;
            
            for (int x = 0; x < blueprint.length; x++) {
                for (int y = 0; y < blueprint[0].length; y++) {
                    for (int z = 0; z < blueprint[0][0].length; z++) {
                        byte layer = blueprint[x][y][z];
                        if (layer == GeodeCell.Layer.SHELL.ordinal()) shellCount++;
                        if (layer == GeodeCell.Layer.CRYSTAL.ordinal()) crystalCount++;
                        if (layer == GeodeCell.Layer.HOLLOW.ordinal()) hollowCount++;
                    }
                }
            }
            
            System.out.println("Test Results:");
            System.out.println("  Shell voxels:   " + shellCount);
            System.out.println("  Crystal voxels: " + crystalCount);
            System.out.println("  Hollow voxels:  " + hollowCount);
            
            if (shellCount > 0 && hollowCount > 0) {
                System.out.println("SUCCESS: Geode structure generated successfully.");
            } else {
                System.out.println("FAILURE: Geode structure is empty or invalid.");
            }
            
        } catch (Exception e) {
            System.err.println("CRITICAL FAILURE: Pipeline crashed!");
            e.printStackTrace();
        }
    }
}
