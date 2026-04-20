package minicraft.math;

import java.nio.FloatBuffer;

/**
 * Minimal Matrix4f for the project. Column-major.
 */
public class Matrix4f {
    public float[] m = new float[16];

    public Matrix4f() { identity(); }
    
    public Matrix4f(Matrix4f other) {
        set(other);
    }

    public Matrix4f set(Matrix4f other) {
        System.arraycopy(other.m, 0, this.m, 0, 16);
        return this;
    }

    public Matrix4f identity() {
        for (int i=0; i<16; i++) m[i] = 0;
        m[0] = m[5] = m[10] = m[15] = 1;
        return this;
    }

    public Matrix4f perspective(float fov, float aspect, float near, float far) {
        float h = (float) Math.tan(fov * 0.5f);
        identity();
        m[0] = 1.0f / (h * aspect);
        m[5] = 1.0f / h;
        m[10] = -(far + near) / (far - near);
        m[11] = -1.0f;
        m[14] = -(2.0f * far * near) / (far - near);
        m[15] = 0;
        return this;
    }

    public Matrix4f ortho(float left, float right, float bottom, float top, float near, float far) {
        identity();
        m[0] = 2.0f / (right - left);
        m[5] = 2.0f / (top - bottom);
        m[10] = -2.0f / (far - near);
        m[12] = -(right + left) / (right - left);
        m[13] = -(top + bottom) / (top - bottom);
        m[14] = -(far + near) / (far - near);
        return this;
    }

    public Matrix4f translate(float x, float y, float z) {
        m[12] += m[0] * x + m[4] * y + m[8] * z;
        m[13] += m[1] * x + m[5] * y + m[9] * z;
        m[14] += m[2] * x + m[6] * y + m[10] * z;
        m[15] += m[3] * x + m[7] * y + m[11] * z;
        return this;
    }

    public Matrix4f translate(Vector3f v) {
        return translate(v.x, v.y, v.z);
    }

    public Matrix4f rotate(float angle, Vector3f axis) {
        float sin = (float) Math.sin(angle);
        float cos = (float) Math.cos(angle);
        float C = 1.0f - cos;
        float x = axis.x, y = axis.y, z = axis.z;
        float rm00 = x * x * C + cos;
        float rm01 = y * x * C + z * sin;
        float rm02 = z * x * C - y * sin;
        float rm10 = x * y * C - z * sin;
        float rm11 = y * y * C + cos;
        float rm12 = z * y * C + x * sin;
        float rm20 = x * z * C + y * sin;
        float rm21 = y * z * C - x * sin;
        float rm22 = z * z * C + cos;
        
        float nm00 = m[0] * rm00 + m[4] * rm01 + m[8] * rm02;
        float nm01 = m[1] * rm00 + m[5] * rm01 + m[9] * rm02;
        float nm02 = m[2] * rm00 + m[6] * rm01 + m[10] * rm02;
        float nm03 = m[3] * rm00 + m[7] * rm01 + m[11] * rm02;
        float nm10 = m[0] * rm10 + m[4] * rm11 + m[8] * rm12;
        float nm11 = m[1] * rm10 + m[5] * rm11 + m[9] * rm12;
        float nm12 = m[2] * rm10 + m[6] * rm11 + m[10] * rm12;
        float nm13 = m[3] * rm10 + m[7] * rm11 + m[11] * rm12;
        float nm20 = m[0] * rm20 + m[4] * rm21 + m[8] * rm22;
        float nm21 = m[1] * rm20 + m[5] * rm21 + m[9] * rm22;
        float nm22 = m[2] * rm20 + m[6] * rm21 + m[10] * rm22;
        float nm23 = m[3] * rm20 + m[7] * rm21 + m[11] * rm22;
        
        m[0] = nm00; m[1] = nm01; m[2] = nm02; m[3] = nm03;
        m[4] = nm10; m[5] = nm11; m[6] = nm12; m[7] = nm13;
        m[8] = nm20; m[9] = nm21; m[10] = nm22; m[11] = nm23;
        return this;
    }

    public Matrix4f rotateX(float angle) {
        return rotate(angle, new Vector3f(1, 0, 0));
    }

    public Matrix4f rotateY(float angle) {
        return rotate(angle, new Vector3f(0, 1, 0));
    }

    public Matrix4f rotateZ(float angle) {
        return rotate(angle, new Vector3f(0, 0, 1));
    }

    public Matrix4f scale(float sx, float sy, float sz) {
        m[0] *= sx; m[1] *= sx; m[2] *= sx; m[3] *= sx;
        m[4] *= sy; m[5] *= sy; m[6] *= sy; m[7] *= sy;
        m[8] *= sz; m[9] *= sz; m[10] *= sz; m[11] *= sz;
        return this;
    }

    /**
     * Inverts this matrix. Uses the standard analytic method (adjugate / determinant).
     */
    public Matrix4f invert() {
        float[] res = new float[16];
        float n00 = m[5] * m[10] * m[15] - m[5] * m[11] * m[14] - m[9] * m[6] * m[15] + m[9] * m[7] * m[14] + m[13] * m[6] * m[11] - m[13] * m[7] * m[10];
        float n01 = -m[1] * m[10] * m[15] + m[1] * m[11] * m[14] + m[9] * m[2] * m[15] - m[9] * m[3] * m[14] - m[13] * m[2] * m[11] + m[13] * m[3] * m[10];
        float n02 = m[1] * m[6] * m[15] - m[1] * m[7] * m[14] - m[5] * m[2] * m[15] + m[5] * m[3] * m[14] + m[13] * m[2] * m[7] - m[13] * m[3] * m[6];
        float n03 = -m[1] * m[6] * m[11] + m[1] * m[7] * m[10] + m[5] * m[2] * m[11] - m[5] * m[3] * m[10] - m[9] * m[2] * m[7] + m[9] * m[3] * m[6];
        
        float det = m[0] * n00 + m[4] * n01 + m[8] * n02 + m[12] * n03;
        if (det == 0) return identity(); // Or throw exception
        float invDet = 1.0f / det;

        res[0] = n00 * invDet;
        res[1] = n01 * invDet;
        res[2] = n02 * invDet;
        res[3] = n03 * invDet;
        
        res[4] = (-m[4] * m[10] * m[15] + m[4] * m[11] * m[14] + m[8] * m[6] * m[15] - m[8] * m[7] * m[14] - m[12] * m[6] * m[11] + m[12] * m[7] * m[10]) * invDet;
        res[5] = (m[0] * m[10] * m[15] - m[0] * m[11] * m[14] - m[8] * m[2] * m[15] + m[8] * m[3] * m[14] + m[12] * m[2] * m[11] - m[12] * m[3] * m[10]) * invDet;
        res[6] = (-m[0] * m[6] * m[15] + m[0] * m[7] * m[14] + m[4] * m[2] * m[15] - m[4] * m[3] * m[14] - m[12] * m[2] * m[7] + m[12] * m[3] * m[6]) * invDet;
        res[7] = (m[0] * m[6] * m[11] - m[0] * m[7] * m[10] - m[4] * m[2] * m[11] + m[4] * m[3] * m[10] + m[8] * m[2] * m[7] - m[8] * m[3] * m[6]) * invDet;
        
        res[8] = (m[4] * m[9] * m[15] - m[4] * m[11] * m[13] - m[8] * m[5] * m[15] + m[8] * m[7] * m[13] + m[12] * m[5] * m[11] - m[12] * m[7] * m[9]) * invDet;
        res[9] = (-m[0] * m[9] * m[15] + m[0] * m[11] * m[13] + m[8] * m[1] * m[15] - m[8] * m[3] * m[13] - m[12] * m[1] * m[11] + m[12] * m[3] * m[9]) * invDet;
        res[10] = (m[0] * m[5] * m[15] - m[0] * m[7] * m[13] - m[4] * m[1] * m[15] + m[4] * m[3] * m[13] + m[12] * m[1] * m[7] - m[12] * m[3] * m[5]) * invDet;
        res[11] = (-m[0] * m[5] * m[11] + m[0] * m[7] * m[9] + m[4] * m[1] * m[11] - m[4] * m[3] * m[9] - m[8] * m[1] * m[7] + m[8] * m[3] * m[5]) * invDet;
        
        res[12] = (-m[4] * m[9] * m[14] + m[4] * m[10] * m[13] + m[8] * m[5] * m[14] - m[8] * m[6] * m[13] - m[12] * m[5] * m[10] + m[12] * m[6] * m[9]) * invDet;
        res[13] = (m[0] * m[9] * m[14] - m[0] * m[10] * m[13] - m[8] * m[1] * m[14] + m[8] * m[2] * m[13] + m[12] * m[1] * m[10] - m[12] * m[2] * m[9]) * invDet;
        res[14] = (-m[0] * m[5] * m[14] + m[0] * m[6] * m[13] + m[4] * m[1] * m[14] - m[4] * m[2] * m[13] - m[12] * m[1] * m[6] + m[12] * m[2] * m[5]) * invDet;
        res[15] = (m[0] * m[5] * m[10] - m[0] * m[6] * m[9] - m[4] * m[1] * m[10] + m[4] * m[2] * m[9] + m[8] * m[1] * m[6] - m[8] * m[2] * m[5]) * invDet;
        
        this.m = res;
        return this;
    }

    public void get(FloatBuffer fb) {
        fb.put(m);
    }
}
