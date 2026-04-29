package minicraft.world.weather;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * LightningBolt — represents a single lightning strike event.
 *
 * ── Lifecycle ───────────────────────────────────────────────────────────
 *
 *   DESCENDING  → bolt is travelling downward (visual only, no damage yet)
 *   STRUCK      → bolt has hit the ground; gameplay effects fire exactly once
 *   FADING      → bolt lingers as a bright flash then fades out
 *   DEAD        → bolt is finished; remove from the active list
 *
 * ── Visual geometry ─────────────────────────────────────────────────────
 *
 * The bolt is represented as a tree of {@link Segment}s.  Each segment
 * connects two 3D points (stored as float[3] = {x, y, z}).  The root
 * starts at {@code strikeX, spawnY, strikeZ} and walks downward, adding
 * random XZ jitter at each step.  Branch segments fork off the main trunk
 * at random intervals and decay in intensity.
 *
 * Your renderer reads {@link #getSegments()} and draws each segment as a
 * glowing line whose alpha = {@code segment.intensity * bolt.getVisualAlpha()}.
 *
 * ── Gameplay effects ────────────────────────────────────────────────────
 *
 *   • Entity damage — all entities within DAMAGE_RADIUS of the strike point
 *     receive {@link #LIGHTNING_DAMAGE} points.
 *   • Fire — flammable blocks within FIRE_RADIUS of the ground strike are
 *     ignited.
 *   • Shockwave — entities within SHOCKWAVE_RADIUS receive an outward
 *     velocity impulse proportional to inverse distance.
 */
public class LightningBolt {

    // ── Tuning ─────────────────────────────────────────────────────────────

    /** Damage dealt to entities at the exact strike point (falls off with distance). */
    public static final float LIGHTNING_DAMAGE = 8f;

    /** Radius (blocks) in which entities receive direct lightning damage. */
    public static final float DAMAGE_RADIUS = 3.5f;

    /** Radius (blocks) in which flammable blocks are set on fire. */
    public static final float FIRE_RADIUS = 2.5f;

    /** Radius (blocks) of the kinetic shockwave impulse. */
    public static final float SHOCKWAVE_RADIUS = 6f;

    /** Peak outward velocity added to entities at the edge of SHOCKWAVE_RADIUS. */
    public static final float SHOCKWAVE_STRENGTH = 0.9f;

    /** Y above the strike point where the bolt originates (sky spawn height). */
    private static final float BOLT_SPAWN_HEIGHT = 60f;

    /** Vertical distance per bolt segment (voxels). */
    private static final float SEGMENT_STEP_Y = 3f;

    /** Maximum XZ jitter per step. Controls how jagged the bolt looks. */
    private static final float JITTER_XZ = 1.8f;

    /** Probability [0,1] that any given step spawns a branch. */
    private static final double BRANCH_CHANCE = 0.18;

    /** Maximum number of branch segments from a single branch point. */
    private static final int BRANCH_MAX_LENGTH = 6;

    /** How long the DESCENDING phase lasts (seconds). */
    private static final float DESCEND_DURATION = 0.12f;

    /** How long the bolt stays fully bright after striking (seconds). */
    private static final float FLASH_DURATION = 0.08f;

    /** How long the bolt takes to fade out after the flash (seconds). */
    private static final float FADE_DURATION = 0.45f;

    // ── State ──────────────────────────────────────────────────────────────

    public enum Phase { DESCENDING, STRUCK, FADING, DEAD }

    private Phase phase = Phase.DESCENDING;
    private float phaseTimer = 0f;

    /** World position of the ground strike point. */
    public final float strikeX, strikeY, strikeZ;

    /** Bolt segments for rendering. Populated in constructor. */
    private final List<Segment> segments = new ArrayList<>();

    // ── Constructor ────────────────────────────────────────────────────────

    /**
     * @param strikeX  World X of the ground strike point
     * @param strikeY  World Y of the ground strike point (top of surface block)
     * @param strikeZ  World Z of the ground strike point
     * @param seed     Per-bolt random seed for reproducible geometry
     */
    public LightningBolt(float strikeX, float strikeY, float strikeZ, long seed) {
        this.strikeX = strikeX;
        this.strikeY = strikeY;
        this.strikeZ = strikeZ;
        generateGeometry(seed);
    }

    // ── Update ─────────────────────────────────────────────────────────────

    /**
     * Advances bolt state.  Returns true if the bolt just transitioned to
     * STRUCK this frame (so {@link LightningSystem} knows to fire effects).
     *
     * @param dt Delta time in seconds.
     * @return True exactly once: the frame the bolt hits the ground.
     */
    public boolean update(float dt) {
        phaseTimer += dt;
        switch (phase) {
            case DESCENDING:
                if (phaseTimer >= DESCEND_DURATION) {
                    phase = Phase.STRUCK;
                    phaseTimer = 0f;
                    return true; // <── caller fires gameplay effects this frame
                }
                break;
            case STRUCK:
                if (phaseTimer >= FLASH_DURATION) {
                    phase = Phase.FADING;
                    phaseTimer = 0f;
                }
                break;
            case FADING:
                if (phaseTimer >= FADE_DURATION) {
                    phase = Phase.DEAD;
                }
                break;
            case DEAD:
                break;
        }
        return false;
    }

    // ── Rendering helpers ──────────────────────────────────────────────────

    /** Returns all line segments that make up this bolt (trunk + branches). */
    public List<Segment> getSegments() { return segments; }

    /** Current lifecycle phase. */
    public Phase getPhase() { return phase; }

    /** True when this bolt should be removed from the active list. */
    public boolean isDead() { return phase == Phase.DEAD; }

    /**
     * Alpha multiplier [0, 1] for the entire bolt this frame.
     * DESCENDING: ramps from 0 to 1, STRUCK: 1, FADING: 1→0.
     */
    public float getVisualAlpha() {
        switch (phase) {
            case DESCENDING: return Math.min(1f, phaseTimer / DESCEND_DURATION);
            case STRUCK:     return 1f;
            case FADING:     return Math.max(0f, 1f - phaseTimer / FADE_DURATION);
            default:         return 0f;
        }
    }

    /**
     * Fraction [0, 1] of the bolt that has "descended" to the ground.
     * Lets the renderer draw only the top portion during the descent phase.
     */
    public float getDescentProgress() {
        if (phase != Phase.DESCENDING) return 1f;
        return Math.min(1f, phaseTimer / DESCEND_DURATION);
    }

    // ── Geometry generation ────────────────────────────────────────────────

    private void generateGeometry(long seed) {
        Random rng = new Random(seed);
        float spawnY = strikeY + BOLT_SPAWN_HEIGHT;

        // Build the main trunk from sky downward to strike point
        float cx = strikeX, cy = spawnY, cz = strikeZ;

        while (cy > strikeY) {
            float ny = Math.max(strikeY, cy - SEGMENT_STEP_Y);
            float nx = cx + (rng.nextFloat() * 2f - 1f) * JITTER_XZ;
            float nz = cz + (rng.nextFloat() * 2f - 1f) * JITTER_XZ;

            // Converge toward the actual strike point as we near the ground
            float t = 1f - (ny - strikeY) / BOLT_SPAWN_HEIGHT;
            nx = lerp(nx, strikeX, t * t * 0.3f);
            nz = lerp(nz, strikeZ, t * t * 0.3f);

            segments.add(new Segment(cx, cy, cz, nx, ny, nz, 1.0f));

            // Occasionally spawn a branch
            if (rng.nextDouble() < BRANCH_CHANCE && cy - strikeY > SEGMENT_STEP_Y * 2) {
                addBranch(rng, cx, cy, cz, BRANCH_MAX_LENGTH, 0.6f);
            }

            cx = nx; cy = ny; cz = nz;
        }
    }

    private void addBranch(Random rng, float ox, float oy, float oz,
                           int maxSteps, float intensity) {
        if (maxSteps <= 0 || intensity < 0.1f) return;

        float cx = ox, cy = oy, cz = oz;
        // Branches angle outward and slightly downward
        float dxBias = (rng.nextFloat() * 2f - 1f) * 2.2f;
        float dzBias = (rng.nextFloat() * 2f - 1f) * 2.2f;

        int steps = 2 + rng.nextInt(maxSteps - 1);
        for (int i = 0; i < steps; i++) {
            float nx = cx + dxBias + (rng.nextFloat() * 2f - 1f) * JITTER_XZ;
            float ny = cy - SEGMENT_STEP_Y * 0.7f; // branches descend slower
            float nz = cz + dzBias + (rng.nextFloat() * 2f - 1f) * JITTER_XZ;

            segments.add(new Segment(cx, cy, cz, nx, ny, nz, intensity));
            cx = nx; cy = ny; cz = nz;
            intensity *= 0.75f; // branches dim as they extend
        }
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    // ── Segment ────────────────────────────────────────────────────────────

    /**
     * A single line segment of the bolt.
     * Renderer: draw a glowing line from (x1,y1,z1) to (x2,y2,z2)
     * with alpha = intensity * bolt.getVisualAlpha().
     * Trunk segments have intensity 1.0; branch segments decay toward 0.
     */
    public static final class Segment {
        public final float x1, y1, z1;
        public final float x2, y2, z2;
        /** [0,1] relative brightness of this segment. */
        public final float intensity;

        Segment(float x1, float y1, float z1,
                float x2, float y2, float z2,
                float intensity) {
            this.x1 = x1; this.y1 = y1; this.z1 = z1;
            this.x2 = x2; this.y2 = y2; this.z2 = z2;
            this.intensity = intensity;
        }
    }
}