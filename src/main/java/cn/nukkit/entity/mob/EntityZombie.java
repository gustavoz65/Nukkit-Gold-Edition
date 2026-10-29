package cn.nukkit.entity.mob;

import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;

/** Zumbi com identidade de rede da familia MCPE 0.15. */
public class EntityZombie extends EntityMob {
    public static final int NETWORK_ID = 32;
    public EntityZombie(FullChunk chunk, CompoundTag nbt) { super(chunk, nbt); }
    @Override public int getNetworkId() { return NETWORK_ID; }
    @Override public String getName() { return hasCustomName() ? getNameTag() : "Zumbi"; }
    @Override protected boolean burnsInDaylight() { return true; }

    @Override
    public Item[] getDrops() {
        int carne = randomDrop(2);
        return carne == 0 ? new Item[0] : new Item[]{Item.get(Item.ROTTEN_FLESH, 0, carne)};
    }
}
