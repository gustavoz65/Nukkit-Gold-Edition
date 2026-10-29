package cn.nukkit.entity.mob;

import cn.nukkit.Player;
import cn.nukkit.entity.data.ByteEntityData;
import cn.nukkit.entity.weather.EntityLightningStrike;
import cn.nukkit.event.entity.CreeperPowerEvent;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.AddEntityPacket;

/**
 * @author Box.
 */
public class EntityCreeper extends EntityMob {
    public static final int NETWORK_ID = 33;

    public static final int DATA_SWELL_DIRECTION = 16;
    public static final int DATA_SWELL = 17;
    public static final int DATA_SWELL_OLD = 18;
    public static final int DATA_POWERED = 19;
    private int goldFuse;

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    public EntityCreeper(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    public boolean isPowered() {
        return getDataPropertyBoolean(DATA_POWERED);
    }

    public void setPowered(EntityLightningStrike bolt) {
        CreeperPowerEvent ev = new CreeperPowerEvent(this, bolt, CreeperPowerEvent.PowerCause.LIGHTNING);
        this.getServer().getPluginManager().callEvent(ev);

        if (!ev.isCancelled()) {
            this.setDataProperty(new ByteEntityData(DATA_POWERED, 1));
            this.namedTag.putBoolean("powered", true);
        }
    }

    public void setPowered(boolean powered) {
        CreeperPowerEvent ev = new CreeperPowerEvent(this, powered ? CreeperPowerEvent.PowerCause.SET_ON : CreeperPowerEvent.PowerCause.SET_OFF);
        this.getServer().getPluginManager().callEvent(ev);

        if (!ev.isCancelled()) {
            this.setDataProperty(new ByteEntityData(DATA_POWERED, powered ? 1 : 0));
            this.namedTag.putBoolean("powered", powered);
        }
    }

    @Override
    protected void initEntity() {
        super.initEntity();

        if (this.namedTag.getBoolean("powered") || this.namedTag.getBoolean("IsPowered")) {
            this.dataProperties.putBoolean(DATA_POWERED, true);
        }
    }

    @Override
    public void spawnTo(Player player) {
        super.spawnTo(player);
    }

    @Override protected double getAttackReach() { return 3; }
    @Override protected int getAttackInterval() { return 1; }

    @Override
    public boolean onUpdate(int currentTick) {
        boolean active = super.onUpdate(currentTick);
        if (this.target == null || this.target.distanceSquared(this) > 9) { this.goldFuse = 0; }
        return active;
    }

    @Override
    protected void attackTarget(Player player) {
        if (++this.goldFuse < 30) { return; }
        this.goldFuse = 0;
        cn.nukkit.event.entity.EntityExplosionPrimeEvent event = new cn.nukkit.event.entity.EntityExplosionPrimeEvent(this, isPowered() ? 6 : 3);
        this.server.getPluginManager().callEvent(event);
        if (event.isCancelled()) { return; }
        cn.nukkit.level.Explosion explosion = new cn.nukkit.level.Explosion(this, event.getForce(), this);
        if (event.isBlockBreaking()) { explosion.explodeA(); }
        explosion.explodeB();
        this.close();
    }
}
