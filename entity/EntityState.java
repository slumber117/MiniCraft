package minicraft.entity;

/**
 * State machine states shared by all entities.
 */
public enum EntityState {
    IDLE,       // Standing still, looking around
    WANDERING,  // Moving around randomly
    FLEEING,    // Running away from threat
    CHASING,    // Pursuing a target
    ATTACKING,  // In melee attack range, swinging
    EATING,     // Grazing / consuming food
    SLEEPING,   // Night-time rest (reduces aggro)
    SWIMMING,   // In water
    FLYING,     // Airborne (Eagle only)
    DEAD
}
