package minicraft.math;

public class Vector4f {
    public float x, y, z, w;

    public Vector4f() { this(0, 0, 0, 0); }
    public Vector4f(float x, float y, float z, float w) {
        this.x = x; this.y = y; this.z = z; this.w = w;
    }

    public Vector4f(Vector4f other) {
        this.x = other.x; this.y = other.y; this.z = other.z; this.w = other.w;
    }

    public Vector4f mul(float scalar) {
        this.x *= scalar; this.y *= scalar; this.z *= scalar; this.w *= scalar;
        return this;
    }
}
