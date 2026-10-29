package cn.nukkit.entity.mob;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.AddEntityPacket;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Base dos mobs hostis do Nukkit-Gold.
 *
 * <p>A IA e propositalmente simples (sem A*), mas cobre o que o jogador percebe:
 * vagar quando nao ha alvo, perseguir com linha de visao, contornar obstaculo
 * baixo pulando, desviar quando fica preso, evitar precipicio e lava ao vagar,
 * respeitar o knockback, queimar ao sol (quem queima) e sumir longe dos jogadores.
 * Para comparacao: o Genisys 0.15 tinha esse codigo escrito, mas desativado por um
 * {@code return} no inicio do onUpdate do zumbi, entao os mobs ficavam parados.</p>
 *
 * <p>Custo: busca de alvo a cada 10 ticks, checagens de bloco so ao escolher
 * direcao, e despawn/sol uma vez por segundo.</p>
 */
public abstract class EntityMob extends EntityCreature {
    private static final double FOLLOW_RANGE = 16;
    private static final int DESPAWN_FAR = 128;
    private static final int DESPAWN_RANDOM = 32;

    protected Player target;
    private int nextAttack;
    private Player revengeTarget;
    private int revengeUntil;

    private double wanderX = Double.NaN;
    private double wanderZ;
    private int wanderUntil;
    private int idleUntil;

    private double lastX;
    private double lastZ;
    private int stuckTicks;
    private double sidestepX;
    private double sidestepZ;
    private int sidestepUntil;

    public EntityMob(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public float getWidth() { return 0.6f; }

    @Override
    public float getHeight() { return 1.8f; }

    @Override
    protected double getStepHeight() { return 0.6; }

    /** Velocidade de perseguicao em blocos por tick. */
    protected double getMoveSpeed() { return 0.13; }
    protected double getAttackReach() { return 1.8; }
    protected int getAttackInterval() { return 20; }

    /** Dano por dificuldade, como no jogo original (facil/normal/dificil). */
    protected float getAttackDamage() {
        switch (this.server.getDifficulty()) {
            case 1: return 2;
            case 3: return 4;
            default: return 3;
        }
    }

    /** Zumbi e esqueleto queimam ao sol. */
    protected boolean burnsInDaylight() { return false; }

    /** Aranha so ataca no escuro, a menos que tenha sido agredida. */
    protected boolean isHostileTo(Player player) { return true; }

    /** Distancia minima que o mob tenta manter do alvo (esqueleto recua). */
    protected double getPreferredMinDistance() { return 0; }

    /** Aranha sobe paredes. */
    protected boolean canClimbWalls() { return false; }

    protected void attackTarget(Player player) {
        player.attack(new EntityDamageByEntityEvent(this, player, EntityDamageEvent.CAUSE_ENTITY_ATTACK, getAttackDamage()));
    }

    @Override
    public void attack(EntityDamageEvent source) {
        super.attack(source);
        if (!source.isCancelled() && source instanceof EntityDamageByEntityEvent) {
            Entity damager = ((EntityDamageByEntityEvent) source).getDamager();
            if (damager instanceof Player) {
                // Retaliacao por 10 segundos, mesmo para mobs neutros (aranha de dia).
                this.revengeTarget = (Player) damager;
                this.revengeUntil = this.server.getTick() + 200;
            }
        }
    }

    private boolean canTarget(Player player) {
        return player != null && player.spawned && player.isAlive() && player.isConnected()
                && player.getLevel() == this.level && !player.isCreative() && !player.isSpectator()
                && player.distanceSquared(this) <= FOLLOW_RANGE * FOLLOW_RANGE;
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

    /** Luz efetiva aproximada: luz de bloco ou do ceu, escurecida a noite. */
    protected int getLightLevel() {
        int x = this.getFloorX();
        int y = Math.max(0, Math.min(127, this.getFloorY()));
        int z = this.getFloorZ();
        if (!this.level.isChunkLoaded(x >> 4, z >> 4)) { return 0; }
        FullChunk chunk = this.level.getChunk(x >> 4, z >> 4, false);
        int sky = chunk.getBlockSkyLight(x & 15, y, z & 15);
        if (!isDaytime()) { sky = Math.max(0, sky - 11); }
        return Math.max(sky, this.level.getBlockLightAt(x, y, z));
    }

    protected boolean isDaytime() {
        int time = Math.abs(this.level.getTime() % 24000);
        return time < 12300 || time > 23850;
    }

    private void selectTarget(int currentTick) {
        if (this.revengeTarget != null && (currentTick > this.revengeUntil || !canTarget(this.revengeTarget))) {
            this.revengeTarget = null;
        }
        if (this.revengeTarget != null) {
            this.target = this.revengeTarget;
            return;
        }
        Player best = null;
        double nearest = FOLLOW_RANGE * FOLLOW_RANGE;
        for (Player candidate : this.level.getPlayers().values()) {
            double distance = candidate.distanceSquared(this);
            if (distance < nearest && canTarget(candidate) && isHostileTo(candidate)) {
                best = candidate;
                nearest = distance;
            }
        }
        // So comeca a perseguir quem consegue ver; quem ja e alvo continua sendo seguido.
        if (best != null && best != this.target && !clearPathTo(best)) {
            best = null;
        }
        this.target = best;
    }

    @Override
    public boolean onUpdate(int currentTick) {
        boolean updated = super.onUpdate(currentTick);
        if (this.closed || !this.isAlive()) { return updated; }
        if (this.server.getDifficulty() == 0) { this.close(); return false; }

        if (currentTick % 20 == 0) {
            if (shouldDespawn()) { this.close(); return false; }
            if (burnsInDaylight() && isDaytime() && !this.isInsideOfWater() && seesSky()) {
                this.setOnFire(8);
            }
        }

        if (this.target == null || !canTarget(this.target) || !isHostileTo(this.target) && this.target != this.revengeTarget
                || currentTick % 10 == 0) {
            selectTarget(currentTick);
        }

        // Durante o knockback (attackTime > 0) a IA nao sobrescreve o impulso recebido.
        boolean steering = this.attackTime <= 0;
        double desiredX = 0;
        double desiredZ = 0;
        if (this.target != null) {
            double dx = this.target.x - this.x;
            double dz = this.target.z - this.z;
            double distance = Math.sqrt(dx * dx + dz * dz);
            this.yaw = Math.toDegrees(Math.atan2(-dx, dz));
            boolean visible = clearPathTo(this.target);
            boolean inReach = distance <= getAttackReach() && Math.abs(this.target.y - this.y) < 2.5 && visible;
            if (inReach && currentTick >= this.nextAttack) {
                this.nextAttack = currentTick + getAttackInterval();
                attackTarget(this.target);
                if (this.closed) { return false; }
            }
            if (distance > 0.001) {
                double speed = getMoveSpeed();
                if (getPreferredMinDistance() > 0 && distance < getPreferredMinDistance() && visible) {
                    desiredX = -dx / distance * speed;
                    desiredZ = -dz / distance * speed;
                } else if (!inReach) {
                    desiredX = dx / distance * speed;
                    desiredZ = dz / distance * speed;
                }
            }
        } else {
            double[] wander = wander(currentTick);
            desiredX = wander[0];
            desiredZ = wander[1];
        }

        if (currentTick < this.sidestepUntil) {
            desiredX = this.sidestepX;
            desiredZ = this.sidestepZ;
        }

        if (steering) {
            this.motionX = desiredX;
            this.motionZ = desiredZ;
            if (desiredX != 0 || desiredZ != 0) {
                if (this.target == null) { this.yaw = Math.toDegrees(Math.atan2(-desiredX, desiredZ)); }
                if (this.isCollidedHorizontally) {
                    if (canClimbWalls()) {
                        this.motionY = 0.2;
                    } else if (this.onGround && hasHeadroom()) {
                        this.motionY = 0.42;
                    }
                }
            }
        }

        if (this.isInsideOfWater()) {
            this.motionY = Math.min(this.motionY + 0.05, 0.1);
        } else if (this.onGround && this.motionY < 0) {
            this.motionY = 0;
        } else {
            this.motionY -= getGravity();
        }

        // A IA usa somente chunks carregados; nao gera terreno ao perseguir jogadores.
        int nextX = (int) Math.floor(this.x + this.motionX);
        int nextZ = (int) Math.floor(this.z + this.motionZ);
        if (this.level.isChunkLoaded(nextX >> 4, nextZ >> 4)) {
            this.move(this.motionX, this.motionY, this.motionZ);
        } else {
            this.motionX = this.motionZ = 0;
        }

        trackStuck(currentTick, desiredX, desiredZ);

        if (!steering) {
            double friction = this.onGround ? 0.6 : 0.91;
            this.motionX *= friction;
            this.motionZ *= friction;
        }
        this.motionY *= 0.98;
        this.updateMovement();
        return true;
    }

    /** Vaga ate um ponto aleatorio proximo; as vezes fica parado, como no jogo original. */
    private double[] wander(int currentTick) {
        if (currentTick < this.idleUntil) { return new double[]{0, 0}; }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (Double.isNaN(this.wanderX) || currentTick >= this.wanderUntil) {
            if (random.nextInt(10) < 4) {
                this.wanderX = Double.NaN;
                this.idleUntil = currentTick + random.nextInt(40, 121);
                return new double[]{0, 0};
            }
            this.wanderX = this.x + random.nextInt(-8, 9);
            this.wanderZ = this.z + random.nextInt(-8, 9);
            this.wanderUntil = currentTick + random.nextInt(80, 161);
        }
        double dx = this.wanderX - this.x;
        double dz = this.wanderZ - this.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 0.5) {
            this.wanderX = Double.NaN;
            this.idleUntil = currentTick + random.nextInt(40, 121);
            return new double[]{0, 0};
        }
        double speed = getMoveSpeed() * 0.6;
        double vx = dx / distance * speed;
        double vz = dz / distance * speed;
        if (!isSafeStep(vx, vz)) {
            this.wanderX = Double.NaN;
            this.idleUntil = currentTick + 20;
            return new double[]{0, 0};
        }
        return new double[]{vx, vz};
    }

    /** Nao vaga para precipicio (mais de 3 blocos) nem para lava. */
    private boolean isSafeStep(double vx, double vz) {
        double length = Math.sqrt(vx * vx + vz * vz);
        if (length == 0) { return true; }
        int x = (int) Math.floor(this.x + vx / length * 0.8);
        int z = (int) Math.floor(this.z + vz / length * 0.8);
        int y = this.getFloorY();
        if (!this.level.isChunkLoaded(x >> 4, z >> 4)) { return false; }
        for (int depth = 0; depth <= 3; depth++) {
            int id = this.level.getBlockIdAt(x, y - depth, z);
            if (id == Block.LAVA || id == Block.STILL_LAVA || id == Block.FIRE) { return false; }
            if (Block.solid[id]) { return true; }
        }
        return false;
    }

    private boolean hasHeadroom() {
        int x = this.getFloorX();
        int z = this.getFloorZ();
        int y = (int) Math.floor(this.y + getHeight() + 0.5);
        return y > 127 || !Block.solid[this.level.getBlockIdAt(x, y, z)];
    }

    private boolean seesSky() {
        int x = this.getFloorX();
        int z = this.getFloorZ();
        int y = Math.max(0, Math.min(127, (int) Math.floor(this.y + getEyeHeight())));
        if (!this.level.isChunkLoaded(x >> 4, z >> 4)) { return false; }
        return this.level.getChunk(x >> 4, z >> 4, false).getBlockSkyLight(x & 15, y, z & 15) >= 15;
    }

    /** Se tenta andar e nao sai do lugar por 1 segundo, desvia para o lado por 1 segundo. */
    private void trackStuck(int currentTick, double desiredX, double desiredZ) {
        double moved = Math.abs(this.x - this.lastX) + Math.abs(this.z - this.lastZ);
        this.lastX = this.x;
        this.lastZ = this.z;
        if ((desiredX == 0 && desiredZ == 0) || moved > 0.02 || currentTick < this.sidestepUntil) {
            this.stuckTicks = 0;
            return;
        }
        if (++this.stuckTicks < 20) { return; }
        this.stuckTicks = 0;
        double side = ThreadLocalRandom.current().nextBoolean() ? 1 : -1;
        this.sidestepX = -desiredZ * side;
        this.sidestepZ = desiredX * side;
        this.sidestepUntil = currentTick + 20;
        this.wanderX = Double.NaN;
    }

    /**
     * Despawn do jogo original: some longe de todos os jogadores (128 blocos) e,
     * entre 32 e 128, tem chance de sumir. Mob com nome nunca some.
     */
    private boolean shouldDespawn() {
        if (this.hasCustomName() || this.namedTag.getBoolean("PersistenceRequired") || this.level.getPlayers().isEmpty()) {
            return false;
        }
        double nearest = Double.MAX_VALUE;
        for (Player player : this.level.getPlayers().values()) {
            if (player.spawned) { nearest = Math.min(nearest, player.distanceSquared(this)); }
        }
        if (nearest == Double.MAX_VALUE) { return false; }
        if (nearest > DESPAWN_FAR * DESPAWN_FAR) { return true; }
        // 1/800 por tick no jogo original; aqui verificado 1x por segundo (20/800 = 1/40).
        return nearest > DESPAWN_RANDOM * DESPAWN_RANDOM && ThreadLocalRandom.current().nextInt(40) == 0;
    }

    /** Quantidade aleatoria de 0 a max, como os drops comuns de mobs. */
    protected static int randomDrop(int max) {
        return ThreadLocalRandom.current().nextInt(max + 1);
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
