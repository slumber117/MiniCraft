package minicraft.world.behavior;

import minicraft.Main;
import minicraft.world.World;
import minicraft.world.BlockInteraction;

/**
 * Handles interaction logic for ship consoles.
 */
public class ConsoleBlock implements BlockInteraction {

    @Override
    public void onInteract(Main main, World world, int gx, int gy, int gz) {
        main.drydockX = gx;
        main.drydockY = gy;
        main.drydockZ = gz;
        main.shipConsoleOpen = true;
    }
}
