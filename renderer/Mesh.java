package minicraft.renderer;

import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Optimized Mesh: Uses manual native memory allocation for large buffers
 * to avoid LWJGL MemoryStack (64KB) overflow errors.
 */
public class Mesh {

    private final int vaoId;
    private final List<Integer> vboIdList;
    private final int vertexCount;
    private Texture texture;
    private int uvVboId;

    public Mesh(float[] positions, float[] uvs, float[] lightInfo, int[] indices, Texture texture) {
        this.texture = texture;
        this.vertexCount = indices.length;
        this.vboIdList = new ArrayList<>();

        FloatBuffer posBuffer = null;
        FloatBuffer uvBuffer = null;
        FloatBuffer lightBuffer = null;
        IntBuffer indicesBuffer = null;

        try {
            vaoId = GL30.glGenVertexArrays();
            GL30.glBindVertexArray(vaoId);

            // 1. Position
            int vboId = GL30.glGenBuffers();
            vboIdList.add(vboId);
            posBuffer = MemoryUtil.memAllocFloat(positions.length);
            posBuffer.put(positions).flip();
            GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, vboId);
            GL30.glBufferData(GL30.GL_ARRAY_BUFFER, posBuffer, GL30.GL_STATIC_DRAW);
            GL30.glEnableVertexAttribArray(0);
            GL30.glVertexAttribPointer(0, 3, GL30.GL_FLOAT, false, 0, 0);

            // 2. UVs
            uvVboId = GL30.glGenBuffers();
            vboIdList.add(uvVboId);
            uvBuffer = MemoryUtil.memAllocFloat(uvs.length);
            uvBuffer.put(uvs).flip();
            GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, uvVboId);
            GL30.glBufferData(GL30.GL_ARRAY_BUFFER, uvBuffer, GL30.GL_DYNAMIC_DRAW);
            GL30.glEnableVertexAttribArray(1);
            GL30.glVertexAttribPointer(1, 2, GL30.GL_FLOAT, false, 0, 0);

            // 3. LightInfo
            vboId = GL30.glGenBuffers();
            vboIdList.add(vboId);
            if (lightInfo == null) {
                lightInfo = new float[positions.length / 3 * 2];
                java.util.Arrays.fill(lightInfo, 1.0f);
            }
            lightBuffer = MemoryUtil.memAllocFloat(lightInfo.length);
            lightBuffer.put(lightInfo).flip();
            GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, vboId);
            GL30.glBufferData(GL30.GL_ARRAY_BUFFER, lightBuffer, GL30.GL_STATIC_DRAW);
            GL30.glEnableVertexAttribArray(2);
            GL30.glVertexAttribPointer(2, 2, GL30.GL_FLOAT, false, 0, 0);

            // Index VBO
            vboId = GL30.glGenBuffers();
            vboIdList.add(vboId);
            indicesBuffer = MemoryUtil.memAllocInt(indices.length);
            indicesBuffer.put(indices).flip();
            GL30.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, vboId);
            GL30.glBufferData(GL30.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL30.GL_STATIC_DRAW);

            GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0);
            GL30.glBindVertexArray(0);
        } finally {
            // Free native memory immediately after upload to GPU
            if (posBuffer != null) MemoryUtil.memFree(posBuffer);
            if (uvBuffer != null) MemoryUtil.memFree(uvBuffer);
            if (lightBuffer != null) MemoryUtil.memFree(lightBuffer);
            if (indicesBuffer != null) MemoryUtil.memFree(indicesBuffer);
        }
    }

    public Mesh(float[] positions, float[] textCoords, int[] indices, Texture texture) {
        this(positions, textCoords, null, indices, texture);
    }

    public void setUVs(float[] uvs) {
        FloatBuffer buffer = null;
        try {
            buffer = MemoryUtil.memAllocFloat(uvs.length);
            buffer.put(uvs).flip();
            GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, uvVboId);
            GL30.glBufferSubData(GL30.GL_ARRAY_BUFFER, 0, buffer);
        } finally {
            if (buffer != null) MemoryUtil.memFree(buffer);
        }
    }

    public int getVaoId() { return vaoId; }
    public int getVertexCount() { return vertexCount; }
    public void setTexture(Texture texture) { this.texture = texture; }

    public void render() { render(this.texture); }
    public void render(Texture texture) {
        GL30.glActiveTexture(GL30.GL_TEXTURE0);
        if (texture != null) texture.bind();
        else GL30.glBindTexture(GL30.GL_TEXTURE_2D, 0);

        GL30.glBindVertexArray(getVaoId());
        GL30.glDrawElements(GL30.GL_TRIANGLES, getVertexCount(), GL30.GL_UNSIGNED_INT, 0);
        GL30.glBindVertexArray(0);
    }

    public void cleanup() {
        GL30.glDisableVertexAttribArray(0);
        GL30.glDisableVertexAttribArray(1);
        GL30.glDisableVertexAttribArray(2);
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0);
        for (int vboId : vboIdList) GL30.glDeleteBuffers(vboId);
        GL30.glBindVertexArray(0);
        GL30.glDeleteVertexArrays(vaoId);
    }
}
