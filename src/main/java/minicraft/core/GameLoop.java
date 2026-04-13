package minicraft.core;

/**
 * GameLoop — decouples simulation ticks from render frames.
 */
public class GameLoop {

    public final float TICK_DT;
    private long tickCount = 0;
    private float accumulator = 0f;
    private final float MAX_ACCUMULATOR;
    private final java.util.List<Tickable> tickables = new java.util.ArrayList<>();

    public GameLoop(int tickRate) {
        this.TICK_DT         = 1.0f / tickRate;
        this.MAX_ACCUMULATOR = TICK_DT * 5;
    }

    public void update(float deltaTime) {
        accumulator += Math.min(deltaTime, MAX_ACCUMULATOR);

        while (accumulator >= TICK_DT) {
            for (Tickable t : tickables) {
                t.tick(TICK_DT);
            }
            tickCount++;
            accumulator -= TICK_DT;
        }
    }

    public float getInterpolationAlpha() {
        return accumulator / TICK_DT;
    }

    public void addTickable(Tickable tickable) {
        tickables.add(tickable);
    }

    public void removeTickable(Tickable tickable) {
        tickables.remove(tickable);
    }

    public long getTickCount() { return tickCount; }

    @FunctionalInterface
    public interface Tickable {
        void tick(float dt);
    }
}
