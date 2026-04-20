package minicraft.world;

import minicraft.Main;

/**
 * Defines a block's reaction to being right-clicked.
 */
public interface BlockInteraction {
    void onInteract(Main main, World world, int gx, int gy, int gz);
}
