package cn.nukkit.entity.mob;

import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;

public class EntitySpider extends EntityMob {
    public static final int NETWORK_ID = 35;
    public EntitySpider(FullChunk chunk, CompoundTag nbt) { super(chunk, nbt); }
    @Override public int getNetworkId() { return NETWORK_ID; }
    @Override public float getWidth() { return 1.4f; }
    @Override public float getHeight() { return 0.9f; }
    @Override public String getName() { return hasCustomName() ? getNameTag() : "Aranha"; }
    @Override public Item[] getDrops() { return new Item[]{Item.get(Item.STRING)}; }
    @Override protected void initEntity() {
        super.initEntity();
        setMaxHealth(16);
        setHealth(Math.min(getHealth(), 16));
    }
}
