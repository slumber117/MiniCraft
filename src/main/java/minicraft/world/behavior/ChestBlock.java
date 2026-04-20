package minicraft.world.behavior;

import minicraft.Main;
import minicraft.world.World;
import minicraft.world.BlockInteraction;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Handles interaction logic for storage chests.
 */
public class ChestBlock implements BlockInteraction {

    @Override
    public void onInteract(Main main, World world, int gx, int gy, int gz) {
        main.activeChest = world.getContainer(gx, gy, gz);
        main.chestOpen = true;
        glfwSetInputMode(main.getWindow(), GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
    }
}
