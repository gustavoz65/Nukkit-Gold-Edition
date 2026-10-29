package cn.nukkit.entity.mob;

import cn.nukkit.Player;
import cn.nukkit.entity.projectile.EntityArrow;
import cn.nukkit.event.entity.ProjectileLaunchEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.MobSpawning;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Esqueleto: atira de longe e recua quando o jogador chega perto demais. */
public class EntitySkeleton extends EntityMob {
    public static final int NETWORK_ID = 34;
    public EntitySkeleton(FullChunk chunk, CompoundTag nbt) { super(chunk, nbt); }
    @Override public int getNetworkId() { return NETWORK_ID; }
    @Override public String getName() { return hasCustomName() ? getNameTag() : "Esqueleto"; }
    @Override protected double getMoveSpeed() { return 0.12; }
    @Override protected double getAttackReach() { return 12; }
    @Override protected int getAttackInterval() { return 40; }
    @Override protected double getPreferredMinDistance() { return 4; }
    @Override protected boolean burnsInDaylight() { return true; }

    @Override
    public Item[] getDrops() {
        List<Item> drops = new ArrayList<>();
        int ossos = randomDrop(2);
        int flechas = randomDrop(2);
        if (ossos > 0) { drops.add(Item.get(Item.BONE, 0, ossos)); }
        if (flechas > 0) { drops.add(Item.get(Item.ARROW, 0, flechas)); }
        return drops.toArray(new Item[0]);
    }

    @Override
    protected void attackTarget(Player player) {
        Vector3 origin = this.add(0, getEyeHeight(), 0);
        Vector3 aim = player.add(0, player.getEyeHeight(), 0).subtract(origin);
        // Pequena compensacao da queda da flecha e imprecisao, como no jogo original.
        double distance = Math.sqrt(aim.x * aim.x + aim.z * aim.z);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        aim = aim.add((random.nextDouble() - 0.5) * 0.6, distance * 0.12, (random.nextDouble() - 0.5) * 0.6);
        Vector3 direction = aim.normalize().multiply(1.5);
        CompoundTag nbt = MobSpawning.createNBT(origin.x, origin.y, origin.z);
        EntityArrow arrow = new EntityArrow(this.chunk, nbt, this);
        arrow.setMotion(direction);
        ProjectileLaunchEvent event = new ProjectileLaunchEvent(arrow);
        this.server.getPluginManager().callEvent(event);
        if (event.isCancelled()) { arrow.close(); } else { arrow.spawnToAll(); }
    }
}
