package minicraft.entity.monsters;

import minicraft.entity.EntityManager;
import minicraft.entity.EntityType;
import minicraft.entity.Player;

public class OnyxDragon extends BaseDragon {
    public OnyxDragon() {
        super(EntityType.ONYX_DRAGON, EntityType.ONYX_DRAGON.baseHealth);
    }

    @Override
    public void onDeath(EntityManager manager, minicraft.world.World world) {
        super.onDeath(manager, world);
        
        // Spawn reward chest at death location
        int ix = (int)Math.floor(position.x);
        int iy = (int)Math.floor(position.y);
        int iz = (int)Math.floor(position.z);
        
        world.setBlock(ix, iy, iz, minicraft.world.Block.CHEST);
        minicraft.entity.Inventory chestInv = world.getOrCreateContainer(ix, iy, iz);
        if (chestInv != null) {
            // Reward: Next Tier (Xanthiosite - Tier 11)
            chestInv.add(new minicraft.item.ArmorItem("Xanthiosite Helmet", "Xanthiosite", 0.6f, 11, 10f, 1.2f, 0.5f, null, minicraft.item.ArmorItem.ArmorSlot.HELMET), 1);
            chestInv.add(new minicraft.item.ArmorItem("Xanthiosite Chestplate", "Xanthiosite", 0.6f, 11, 10f, 1.2f, 0.5f, null, minicraft.item.ArmorItem.ArmorSlot.CHESTPLATE), 1);
            chestInv.add(new minicraft.item.ArmorItem("Xanthiosite Leggings", "Xanthiosite", 0.6f, 11, 10f, 1.2f, 0.5f, null, minicraft.item.ArmorItem.ArmorSlot.LEGGINGS), 1);
            chestInv.add(new minicraft.item.ArmorItem("Xanthiosite Boots", "Xanthiosite", 0.6f, 11, 10f, 1.2f, 0.5f, null, minicraft.item.ArmorItem.ArmorSlot.BOOTS), 1);
            
            chestInv.add(new minicraft.item.ToolItem("Xanthiosite Sword", minicraft.item.ToolItem.ToolType.SWORD, 11, 105.0f, "item_sword_onyx"), 1);
            chestInv.add(new minicraft.item.ToolItem("Xanthiosite Pick", minicraft.item.ToolItem.ToolType.PICKAXE, 11, 105.0f, "item_pick_onyx"), 1);
            chestInv.add(new minicraft.item.ToolItem("Xanthiosite Axe", minicraft.item.ToolItem.ToolType.AXE, 11, 105.0f, "item_pick_onyx"), 1);
            chestInv.add(new minicraft.item.ToolItem("Xanthiosite Shovel", minicraft.item.ToolItem.ToolType.SHOVEL, 11, 105.0f, "item_pick_onyx"), 1);
        }
    }

    @Override
    protected String getRequiredMaterial() { return "Onyx"; }

    @Override
    protected float getBiteDamage() { return 150.0f; } // Colossal damage

    @Override
    protected void fireProjectile(Player p, EntityManager manager) {
        float dx = p.position.x - position.x;
        float dy = (p.position.y + 1.0f) - position.y;
        float dz = p.position.z - position.z;
        float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        float speed = 25.0f;
        if (len > 0) {
            OnyxProjectile op = new OnyxProjectile(this, (dx/len)*speed, (dy/len)*speed, (dz/len)*speed);
            op.setPosition(position.x, position.y, position.z);
            manager.spawn(op);
        }
    }
}
