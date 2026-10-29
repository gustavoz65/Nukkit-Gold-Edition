package cn.nukkit.level;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.mob.EntityMob;
import cn.nukkit.event.entity.CreatureSpawnEvent;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.ListTag;
import java.util.concurrent.ThreadLocalRandom;

/** Spawn limitado a chunks carregados, com cotas por mundo e por rodada. */
public final class MobSpawning {
    private MobSpawning() { }

    public static boolean isSupportedMonster(int id) { return id >= 32 && id <= 35; }

    public static CompoundTag createNBT(double x, double y, double z) {
        return new CompoundTag()
                .putList(new ListTag<DoubleTag>("Pos").add(new DoubleTag("", x)).add(new DoubleTag("", y)).add(new DoubleTag("", z)))
                .putList(new ListTag<DoubleTag>("Motion").add(new DoubleTag("", 0)).add(new DoubleTag("", 0)).add(new DoubleTag("", 0)))
                .putList(new ListTag<FloatTag>("Rotation").add(new FloatTag("", 0)).add(new FloatTag("", 0)));
    }

    public static Entity spawn(Level level, int id, double x, double y, double z, CreatureSpawnEvent.Reason reason) {
        if (!isSupportedMonster(id) || level.getServer().getDifficulty() == 0
                || !level.getServer().getPropertyBoolean("spawn-mobs", true) || y < 1 || y > 125) { return null; }
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        if (!level.isChunkLoaded(bx >> 4, bz >> 4)) { return null; }
        int limit = Math.max(0, (Integer) level.getServer().getConfig("spawn-limits.monsters", 70));
        int count = 0;
        for (Entity entity : level.getEntities()) {
            if (entity instanceof EntityMob && entity.isAlive() && ++count >= limit) { return null; }
        }
        if (limit == 0) { return null; }
        Position position = new Position(x, y, z, level);
        if (!level.getBlock(position.add(0, -1, 0)).isSolid() || level.getBlock(position).isSolid()
                || level.getBlock(position.add(0, 1, 0)).isSolid()) { return null; }
        CreatureSpawnEvent event = new CreatureSpawnEvent(position, id, reason);
        level.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) { return null; }
        FullChunk chunk = level.getChunk(bx >> 4, bz >> 4, false);
        Entity entity = Entity.createEntity(id, chunk, createNBT(x, y, z));
        if (entity == null) { return null; }
        if (level.getCollisionCubes(entity, entity.getBoundingBox(), true).length > 0) {
            entity.close();
            return null;
        }
        entity.namedTag.putBoolean("GoldNatural", reason == CreatureSpawnEvent.Reason.NATURAL);
        entity.spawnToAll();
        return entity;
    }

    public static void tick(Level level, int currentTick) {
        if (currentTick % 200 != 0 || level.getPlayers().isEmpty() || level.getServer().getDifficulty() == 0
                || !level.getServer().getPropertyBoolean("spawn-mobs", true)) { return; }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int attempts = 0;
        int[] types = {32, 33, 34, 35};
        for (Player player : level.getPlayers().values()) {
            if (!player.spawned || !player.isAlive()) { continue; }
            if (++attempts > 8) { return; }
            int x = player.getFloorX() + random.nextInt(-40, 41);
            int z = player.getFloorZ() + random.nextInt(-40, 41);
            if (!level.isChunkLoaded(x >> 4, z >> 4)) { continue; }
            FullChunk chunk = level.getChunk(x >> 4, z >> 4, false);
            int y = chunk.getHighestBlockAt(x & 15, z & 15) + 1;
            if (y < 1 || y > 125 || level.getBlockLightAt(x, y, z) > 7) { continue; }
            long time = level.getTime() % 24000;
            if ((time < 13000 || time > 23000) && chunk.getBlockSkyLight(x & 15, y, z & 15) > 7) { continue; }
            boolean nearPlayer = false;
            for (Player observer : level.getPlayers().values()) {
                double dx = observer.x - x;
                double dz = observer.z - z;
                if (dx * dx + dz * dz < 24 * 24) { nearPlayer = true; break; }
            }
            if (!nearPlayer) { spawn(level, types[random.nextInt(types.length)], x + 0.5, y, z + 0.5, CreatureSpawnEvent.Reason.NATURAL); }
        }
    }
}
