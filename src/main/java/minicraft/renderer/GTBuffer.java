package minicraft.renderer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.glDrawBuffers;
import static org.lwjgl.opengl.GL30.*;
import org.lwjgl.system.MemoryStack;
import java.nio.IntBuffer;

/**
 * G-Buffer for Deferred Rendering.
 * Captures Albedo and Normals in a single geometry pass.
 */
public class GTBuffer {

    private int fboId;
    private int albedoTextureId, normalTextureId, depthTextureId;
    private int width, height;

    public GTBuffer(int width, int height) {
        init(width, height);
    }

    public void init(int width, int height) {
        this.width = width;
        this.height = height;

        fboId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fboId);

        // 1. Albedo (Standard 8-bit RGBA)
        albedoTextureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, albedoTextureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, 0x812F); // GL_CLAMP_TO_EDGE
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, 0x812F);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, albedoTextureId, 0);

        // 2. Normal (Standard 8-bit RGBA)
        normalTextureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, normalTextureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, 0x812F);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, 0x812F);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, normalTextureId, 0);

        // 3. Depth (24-bit)
        depthTextureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, depthTextureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, width, height, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, 0x812F);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, 0x812F);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTextureId, 0);

        // Configure MRT (Multiple Render Targets)
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buffers = stack.ints(GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1);
            glDrawBuffers(buffers);
        }

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("CRITICAL: G-Buffer FBO Incomplete: " + glCheckFramebufferStatus(GL_FRAMEBUFFER));
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, fboId);
        glViewport(0, 0, width, height);
        // Ensure DrawBuffers are set every bind (some drivers lose state)
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buffers = stack.ints(GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1);
            glDrawBuffers(buffers);
        }
    }

    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void cleanup() {
        glDeleteTextures(albedoTextureId);
        glDeleteTextures(normalTextureId);
        glDeleteTextures(depthTextureId);
        glDeleteFramebuffers(fboId);
    }

    public int getAlbedoTexture() { return albedoTextureId; }
    public int getNormalTexture() { return normalTextureId; }
    public int getDepthTexture() { return depthTextureId; }
}
