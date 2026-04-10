package minicraft.renderer;

import minicraft.math.Matrix4f;
import minicraft.math.Vector3f;

public class Camera {

    private final Vector3f position;
    private final Vector3f rotation;
    private final Matrix4f viewMatrix;

    public Camera() {
        position = new Vector3f(0f, 0f, 0f);
        rotation = new Vector3f(0f, 0f, 0f);
        viewMatrix = new Matrix4f();
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(float x, float y, float z) {
        position.x = x;
        position.y = y;
        position.z = z;
    }

    public void movePosition(float offsetX, float offsetY, float offsetZ) {
        if ( offsetZ != 0 ) {
            position.x += (float)Math.sin(Math.toRadians(rotation.y)) * -1.0f * offsetZ;
            position.z += (float)Math.cos(Math.toRadians(rotation.y)) * offsetZ;
        }
        if ( offsetX != 0) {
            position.x += (float)Math.sin(Math.toRadians(rotation.y - 90)) * -1.0f * offsetX;
            position.z += (float)Math.cos(Math.toRadians(rotation.y - 90)) * offsetX;
        }
        position.y += offsetY;
    }

    public Vector3f getRotation() {
        return rotation;
    }

    public void setRotation(float x, float y, float z) {
        rotation.x = x;
        rotation.y = y;
        rotation.z = z;
    }

    public void moveRotation(float offsetX, float offsetY, float offsetZ) {
        rotation.x += offsetX;
        rotation.y += offsetY;
        rotation.z += offsetZ;
    }

    public Matrix4f getViewMatrix() {
        viewMatrix.identity();
        // First do the rotation so camera rotates over its position
        viewMatrix.rotate((float)Math.toRadians(rotation.x), new Vector3f(1, 0, 0))
                  .rotate((float)Math.toRadians(rotation.y), new Vector3f(0, 1, 0));
        // Then do the translation
        viewMatrix.translate(-position.x, -position.y, -position.z);
        return viewMatrix;
    }

    public void handleMouseInput(float dx, float dy) {
        rotation.x -= dy; // Fixed inversion
        rotation.y += dx;
        
        // Clamp pitch to prevent flipping
        if (rotation.x > 89.0f) rotation.x = 89.0f;
        if (rotation.x < -89.0f) rotation.x = -89.0f;
    }
}
