package minicraft.ship;

import org.joml.Vector3f;

/**
 * Describes a single thruster block embedded in a ship schematic.
 */
public final class ThrusterMount {

    public final Vector3f localPosition;
    public final Vector3f thrustDirection;
    public final float maxForce;
    public final ThrustAxis axis;

    public ThrusterMount(Vector3f localPosition, Vector3f thrustDirection, float maxForce, ThrustAxis axis) {
        this.localPosition   = new Vector3f(localPosition);
        this.thrustDirection = new Vector3f(thrustDirection).normalize();
        this.maxForce        = maxForce;
        this.axis            = axis;
    }

    public enum ThrustAxis {
        FORWARD, BACKWARD, LEFT, RIGHT, UP, DOWN
    }

    @Override
    public String toString() {
        return String.format("ThrusterMount[axis=%s, pos=(%.1f,%.1f,%.1f), force=%.0fN]",
            axis, localPosition.x, localPosition.y, localPosition.z, maxForce);
    }
}
