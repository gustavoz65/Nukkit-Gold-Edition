package cn.nukkit.entity.mob;

import cn.nukkit.entity.EntityCreature;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.Player;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.level.Level;
import cn.nukkit.math.Vector3;
import cn.nukkit.network.protocol.AddEntityPacket;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class EntityMob extends EntityCreature {
    protected Player target;
    private int nextAttack;

    public EntityMob(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public float getWidth() { return 0.6f; }

    @Override
    public float getHeight() { return 1.8f; }

    @Override
    protected double getStepHeight() { return 0.6; }

    protected double getAttackReach() { return 1.8; }
    protected int getAttackInterval() { return 20; }
    protected float getAttackDamage() { return 3; }

    protected void attackTarget(Player player) {
        player.attack(new EntityDamageByEntityEvent(this, player, EntityDamageEvent.CAUSE_ENTITY_ATTACK, getAttackDamage()));
    }

    private boolean canTarget(Player player) {
        return player != null && player.spawned && player.isAlive() && player.isConnected()
                && player.getLevel() == this.level && !player.isCreative() && !player.isSpectator()
                && player.distanceSquared(this) <= 256;
    }

    protected boolean clearPathTo(Player player) {
        Vector3 from = this.add(0, getEyeHeight(), 0);
        Vector3 delta = player.add(0, player.getEyeHeight(), 0).subtract(from);
        int steps = Math.max(1, (int) Math.ceil(delta.length() * 2));
        for (int step = 1; step < steps; step++) {
            Vector3 point = from.add(delta.multiply((double) step / steps));
            if (!this.level.isChunkLoaded(point.getFloorX() >> 4, point.getFloorZ() >> 4) || this.level.getBlock(point).isSolid()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean onUpdate(int currentTick) {
        boolean updated = super.onUpdate(currentTick);
        if (this.closed || !this.isAlive()) { return updated; }
        if (this.server.getDifficulty() == 0) { this.close(); return false; }
        if (!canTarget(this.target) || currentTick % 10 == 0) {
            this.target = null;
            double nearest = 256;
            for (Player candidate : this.level.getPlayers().values()) {
                double distance = candidate.distanceSquared(this);
                if (canTarget(candidate) && distance < nearest) { this.target = candidate; nearest = distance; }
            }
        }
        this.motionX = this.motionZ = 0;
        if (this.target != null) {
            double dx = this.target.x - this.x;
            double dz = this.target.z - this.z;
            double distance = Math.sqrt(dx * dx + dz * dz);
            this.yaw = Math.toDegrees(Math.atan2(-dx, dz));
            if (distance > getAttackReach() || !clearPathTo(this.target)) {
                if (distance > 0.001) {
                    this.motionX = dx / distance * 0.12;
                    this.motionZ = dz / distance * 0.12;
                }
                if (this.isCollidedHorizontally && this.onGround) { this.motionY = 0.35; }
            } else if (currentTick >= this.nextAttack) {
                this.nextAttack = currentTick + getAttackInterval();
                attackTarget(this.target);
            }
        }
        if (this.closed || !this.isAlive()) { return false; }
        this.motionY = this.onGround && this.motionY < 0 ? 0 : this.motionY - getGravity();
        // A IA usa somente chunks carregados; nao gera terreno ao perseguir jogadores.
        int nextX = (int) Math.floor(this.x + this.motionX);
        int nextZ = (int) Math.floor(this.z + this.motionZ);
        if (this.level.isChunkLoaded(nextX >> 4, nextZ >> 4)) {
            this.move(this.motionX, this.motionY, this.motionZ);
            this.updateMovement();
        }
        return true;
    }

    @Override
    public void spawnTo(Player player) {
        if (this.hasSpawned.containsKey(player.getLoaderId()) || !player.usedChunks.containsKey(Level.chunkHash(this.chunk.getX(), this.chunk.getZ()))) { return; }
        AddEntityPacket packet = new AddEntityPacket();
        packet.type = this.getNetworkId();
        packet.eid = this.getId();
        packet.x = (float) this.x;
        packet.y = (float) this.y;
        packet.z = (float) this.z;
        packet.yaw = (float) this.yaw;
        packet.pitch = (float) this.pitch;
        packet.metadata = this.dataProperties;
        player.dataPacket(packet);
        super.spawnTo(player);
    }
}
