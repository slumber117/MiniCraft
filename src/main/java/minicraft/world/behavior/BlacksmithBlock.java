package minicraft.world.behavior;

import minicraft.Main;
import minicraft.world.World;
import minicraft.world.BlockInteraction;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Handles interaction logic for the Blacksmith.
 */
public class BlacksmithBlock implements BlockInteraction {

    @Override
    public void onInteract(Main main, World world, int gx, int gy, int gz) {
        main.blacksmithOpen = true;
        main.setStatusMessage("BLACKSMITH STATION ACCESSED");
        glfwSetInputMode(main.getWindow(), GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
    }
}
