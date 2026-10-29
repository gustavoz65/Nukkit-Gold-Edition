package cn.nukkit.entity.mob;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Aranha: sobe paredes e so ataca no escuro, a menos que seja agredida. */
public class EntitySpider extends EntityMob {
    public static final int NETWORK_ID = 35;
    public EntitySpider(FullChunk chunk, CompoundTag nbt) { super(chunk, nbt); }
    @Override public int getNetworkId() { return NETWORK_ID; }
    @Override public float getWidth() { return 1.4f; }
    @Override public float getHeight() { return 0.9f; }
    @Override public String getName() { return hasCustomName() ? getNameTag() : "Aranha"; }
    @Override protected double getMoveSpeed() { return 0.15; }
    @Override protected boolean canClimbWalls() { return true; }
    @Override protected boolean isHostileTo(Player player) { return getLightLevel() < 8; }

    @Override
    public Item[] getDrops() {
        List<Item> drops = new ArrayList<>();
        int linhas = randomDrop(2);
        if (linhas > 0) { drops.add(Item.get(Item.STRING, 0, linhas)); }
        if (ThreadLocalRandom.current().nextInt(3) == 0) { drops.add(Item.get(Item.SPIDER_EYE, 0, 1)); }
        return drops.toArray(new Item[0]);
    }

    @Override protected void initEntity() {
        super.initEntity();
        setMaxHealth(16);
        setHealth(Math.min(getHealth(), 16));
    }
}
