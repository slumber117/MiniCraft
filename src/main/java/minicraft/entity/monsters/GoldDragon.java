package minicraft.entity.monsters;

import minicraft.entity.EntityManager;
import minicraft.entity.EntityType;
import minicraft.entity.Player;

public class GoldDragon extends BaseDragon {
    public GoldDragon() {
        super(EntityType.GOLD_DRAGON, EntityType.GOLD_DRAGON.baseHealth);
    }

    @Override
    public void onDeath(EntityManager manager, minicraft.world.World world) {
        super.onDeath(manager, world);
        
        int ix = (int)Math.floor(position.x);
        int iy = (int)Math.floor(position.y);
        int iz = (int)Math.floor(position.z);
        
        world.setBlock(ix, iy, iz, minicraft.world.Block.GOLDEN_CHEST);
        minicraft.entity.Inventory chestInv = world.getOrCreateContainer(ix, iy, iz);
        if (chestInv != null) {
            // Reward: Diamond Tier (Tier 4)
            chestInv.add(new minicraft.item.ArmorItem("Diamond Helmet", "DIAMOND_ORE", 0.25f, 4, 1.0f, 1.0f, 0.4f, null, minicraft.item.ArmorItem.ArmorSlot.HELMET), 1);
            chestInv.add(new minicraft.item.ArmorItem("Diamond Chestplate", "DIAMOND_ORE", 0.25f, 4, 1.0f, 1.0f, 0.4f, null, minicraft.item.ArmorItem.ArmorSlot.CHESTPLATE), 1);
            chestInv.add(new minicraft.item.ArmorItem("Diamond Leggings", "DIAMOND_ORE", 0.25f, 4, 1.0f, 1.0f, 0.4f, null, minicraft.item.ArmorItem.ArmorSlot.LEGGINGS), 1);
            chestInv.add(new minicraft.item.ArmorItem("Diamond Boots", "DIAMOND_ORE", 0.25f, 4, 1.0f, 1.0f, 0.4f, null, minicraft.item.ArmorItem.ArmorSlot.BOOTS), 1);
            
            chestInv.add(new minicraft.item.ToolItem("Diamond Sword", minicraft.item.ToolItem.ToolType.SWORD, 4, 16.0f, "item_sword_diamond"), 1);
            chestInv.add(new minicraft.item.ToolItem("Diamond Pick", minicraft.item.ToolItem.ToolType.PICKAXE, 4, 16.0f, "item_pick_diamond"), 1);
            chestInv.add(new minicraft.item.ToolItem("Diamond Axe", minicraft.item.ToolItem.ToolType.AXE, 4, 16.0f, "item_axe_diamond"), 1);
            chestInv.add(new minicraft.item.ToolItem("Diamond Shovel", minicraft.item.ToolItem.ToolType.SHOVEL, 4, 16.0f, "item_shovel_diamond"), 1);
        }
    }

    @Override
    protected String getRequiredMaterial() { return "Gold"; }

    @Override
    protected float getBiteDamage() { return 100.0f; } // Colossal damage

    @Override
    protected void fireProjectile(Player p, EntityManager manager) {
        float dx = p.position.x - position.x;
        float dy = (p.position.y + 1.0f) - position.y;
        float dz = p.position.z - position.z;
        float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        float speed = 22.0f;
        if (len > 0) {
            GoldFireball gf = new GoldFireball(this, (dx/len)*speed, (dy/len)*speed, (dz/len)*speed);
            gf.setPosition(position.x, position.y, position.z);
            manager.spawn(gf);
        }
    }
}
