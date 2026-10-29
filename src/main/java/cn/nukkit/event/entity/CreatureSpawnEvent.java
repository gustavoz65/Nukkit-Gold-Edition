package cn.nukkit.event.entity;

import cn.nukkit.event.Cancellable;
import cn.nukkit.event.Event;
import cn.nukkit.event.HandlerList;
import cn.nukkit.level.Position;

/** Permite que protecoes cancelem o spawn antes de criar a entidade. */
public class CreatureSpawnEvent extends Event implements Cancellable {
    public enum Reason { NATURAL, SPAWNER }
    private static final HandlerList handlers = new HandlerList();
    private final Position position;
    private final int networkId;
    private final Reason reason;

    public CreatureSpawnEvent(Position position, int networkId, Reason reason) {
        this.position = position;
        this.networkId = networkId;
        this.reason = reason;
    }

    public static HandlerList getHandlers() { return handlers; }
    public Position getPosition() { return this.position; }
    public int getNetworkId() { return this.networkId; }
    public Reason getReason() { return this.reason; }
}
