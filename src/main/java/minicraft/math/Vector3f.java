package minicraft.math;

public class Vector3f {
    public float x, y, z;

    public Vector3f() { this(0, 0, 0); }
    public Vector3f(float x, float y, float z) {
        this.x = x; this.y = y; this.z = z;
    }
    public Vector3f(Vector3f other) {
        this.x = other.x; this.y = other.y; this.z = other.z;
    }

    public Vector3f set(float x, float y, float z) {
        this.x = x; this.y = y; this.z = z;
        return this;
    }

    public Vector3f set(Vector3f other) {
        this.x = other.x; this.y = other.y; this.z = other.z;
        return this;
    }

    public Vector3f mul(float s) {
        this.x *= s; this.y *= s; this.z *= s;
        return this;
    }

    public Vector3f add(Vector3f v) {
        this.x += v.x; this.y += v.y; this.z += v.z;
        return this;
    }

    public Vector3f sub(Vector3f v) {
        this.x -= v.x; this.y -= v.y; this.z -= v.z;
        return this;
    }

    public Vector3f normalize() {
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        if (len > 0) {
            x /= len; y /= len; z /= len;
        }
        return this;
    }

    public float distanceSquared(Vector3f other) {
        float dx = this.x - other.x;
        float dy = this.y - other.y;
        float dz = this.z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }
}
