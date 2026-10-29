package cn.nukkit.blockentity;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.mob.EntityMob;
import cn.nukkit.event.entity.CreatureSpawnEvent;
import cn.nukkit.level.MobSpawning;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import java.util.concurrent.ThreadLocalRandom;

/** Spawner persistente com limite local e verificacao periodica de jogadores. */
public class BlockEntityMobSpawner extends BlockEntitySpawnable {
    private int ticks;

    public BlockEntityMobSpawner(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
        if (!this.namedTag.contains("EntityId") || !MobSpawning.isSupportedMonster(getEntityId())) { this.namedTag.putInt("EntityId", 32); }
        if (!this.namedTag.contains("Delay")) { this.namedTag.putInt("Delay", 200); }
        this.namedTag.putInt("Delay", Math.max(0, Math.min(12000, this.namedTag.getInt("Delay"))));
        scheduleUpdate();
    }

    public int getEntityId() { return this.namedTag.contains("EntityId") ? this.namedTag.getInt("EntityId") : 32; }

    public boolean setEntityId(int id) {
        if (!MobSpawning.isSupportedMonster(id)) { return false; }
        this.namedTag.putInt("EntityId", id);
        this.chunk.setChanged();
        spawnToAll();
        return true;
    }

    @Override public boolean isBlockEntityValid() { return getBlock().getId() == Block.MONSTER_SPAWNER; }

    @Override
    public boolean onUpdate() {
        if (this.closed || !isBlockEntityValid()) { this.close(); return false; }
        if (++this.ticks % 20 != 0) { return true; }
        boolean active = false;
        for (Player player : this.level.getPlayers().values()) {
            if (player.spawned && player.isAlive() && player.distanceSquared(this) <= 16 * 16) { active = true; break; }
        }
        if (!active) { return true; }
        int delay = this.namedTag.getInt("Delay") - 20;
        if (delay > 0) { this.namedTag.putInt("Delay", delay); return true; }
        this.namedTag.putInt("Delay", ThreadLocalRandom.current().nextInt(200, 801));
        int nearby = 0;
        for (Entity entity : this.level.getNearbyEntities(new cn.nukkit.math.AxisAlignedBB(this.x - 8, this.y - 4, this.z - 8, this.x + 8, this.y + 4, this.z + 8))) {
            if (entity instanceof EntityMob) { ++nearby; }
        }
        for (int attempt = 0; attempt < 4 && nearby < 6; attempt++) {
            double x = this.x + ThreadLocalRandom.current().nextInt(-4, 5) + 0.5;
            double z = this.z + ThreadLocalRandom.current().nextInt(-4, 5) + 0.5;
            if (MobSpawning.spawn(this.level, getEntityId(), x, this.y, z, CreatureSpawnEvent.Reason.SPAWNER) != null) { ++nearby; }
        }
        this.chunk.setChanged();
        return true;
    }

    @Override
    public CompoundTag getSpawnCompound() {
        return new CompoundTag().putString("id", BlockEntity.MOB_SPAWNER)
                .putInt("x", getFloorX()).putInt("y", getFloorY()).putInt("z", getFloorZ())
                .putInt("EntityId", getEntityId());
    }
}
