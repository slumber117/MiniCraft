package minicraft.renderer;

/**
 * Represents a rectangular region within a larger Texture Atlas.
 * Stores normalized UV coordinates [0.0 - 1.0] for the sub-region.
 */
public class TextureRegion {

    private final Texture texture;
    private final float u1, v1, u2, v2;

    public TextureRegion(Texture texture, float u1, float v1, float u2, float v2) {
        this.texture = texture;
        this.u1 = u1;
        this.v1 = v1;
        this.u2 = u2;
        this.v2 = v2;
    }

    public Texture getTexture() {
        return texture;
    }

    public float getU1() { return u1; }
    public float getV1() { return v1; }
    public float getU2() { return u2; }
    public float getV2() { return v2; }

    /**
     * Map a local [0, 1] UV coordinate to the atlas space.
     */
    public float mapU(float u) {
        return u1 + u * (u2 - u1);
    }

    public float mapV(float v) {
        return v1 + v * (v2 - v1);
    }
}
