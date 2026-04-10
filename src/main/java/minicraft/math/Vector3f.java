package minicraft.math;

public class Vector3f {
    public float x, y, z;

    public Vector3f() { this(0, 0, 0); }
    public Vector3f(float x, float y, float z) {
        this.x = x; this.y = y; this.z = z;
    }

    public Vector3f set(float x, float y, float z) {
        this.x = x; this.y = y; this.z = z;
        return this;
    }

    public float distanceSquared(Vector3f other) {
        float dx = this.x - other.x;
        float dy = this.y - other.y;
        float dz = this.z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }
}
