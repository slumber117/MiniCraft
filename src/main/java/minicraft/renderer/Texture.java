package minicraft.renderer;

import org.lwjgl.system.MemoryUtil;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;

public class Texture {

    private final int id;
    private final int width;
    private final int height;

    public Texture(String resourcePath) throws Exception {
        ByteBuffer imageBuffer;

        ByteBuffer rawBuffer = null;
        try (InputStream is = Texture.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new Exception("Texture resource not found: " + resourcePath);
            }
            byte[] bytes = is.readAllBytes();
            rawBuffer = MemoryUtil.memAlloc(bytes.length);
            rawBuffer.put(bytes).flip();

            try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);

                stbi_set_flip_vertically_on_load(true);
                imageBuffer = stbi_load_from_memory(rawBuffer, w, h, channels, 4);
                if (imageBuffer == null) {
                    throw new Exception("Image file [" + resourcePath + "] not loaded: " + stbi_failure_reason());
                }

                this.width = w.get();
                this.height = h.get();
            }
        } finally {
            if (rawBuffer != null) MemoryUtil.memFree(rawBuffer);
        }

        this.id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, this.id);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, this.width, this.height, 0, GL_RGBA, GL_UNSIGNED_BYTE, imageBuffer);
        glGenerateMipmap(GL_TEXTURE_2D);
        stbi_image_free(imageBuffer);
    }

    public Texture(int width, int height, ByteBuffer buf) {
        this.width = width;
        this.height = height;
        this.id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, this.id);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
        glGenerateMipmap(GL_TEXTURE_2D);
    }

    public int getId() {
        return id;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, id);
    }

    public void cleanup() {
        glDeleteTextures(id);
    }
}
