package cn.nukkit.entity.mob;

import cn.nukkit.Player;
import cn.nukkit.entity.projectile.EntityArrow;
import cn.nukkit.event.entity.ProjectileLaunchEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.MobSpawning;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;

public class EntitySkeleton extends EntityMob {
    public static final int NETWORK_ID = 34;
    public EntitySkeleton(FullChunk chunk, CompoundTag nbt) { super(chunk, nbt); }
    @Override public int getNetworkId() { return NETWORK_ID; }
    @Override public String getName() { return hasCustomName() ? getNameTag() : "Esqueleto"; }
    @Override public Item[] getDrops() { return new Item[]{Item.get(Item.BONE), Item.get(Item.ARROW)}; }
    @Override protected double getAttackReach() { return 12; }
    @Override protected int getAttackInterval() { return 40; }

    @Override
    protected void attackTarget(Player player) {
        Vector3 origin = this.add(0, getEyeHeight(), 0);
        Vector3 direction = player.add(0, player.getEyeHeight(), 0).subtract(origin).normalize().multiply(1.4);
        CompoundTag nbt = MobSpawning.createNBT(origin.x, origin.y, origin.z);
        EntityArrow arrow = new EntityArrow(this.chunk, nbt, this);
        arrow.setMotion(direction);
        ProjectileLaunchEvent event = new ProjectileLaunchEvent(arrow);
        this.server.getPluginManager().callEvent(event);
        if (event.isCancelled()) { arrow.close(); } else { arrow.spawnToAll(); }
    }
}
