package cn.nukkit.utils;

/** Limite com reposicao gradual; nao depende dos ticks nem do relogio de parede. */
public final class RateLimiter {
    private final int capacity;
    private final int perSecond;
    private double tokens;
    private long lastNanos;

    public RateLimiter(int capacity, int perSecond) {
        this(capacity, perSecond, System.nanoTime());
    }

    public RateLimiter(int capacity, int perSecond, long now) {
        if (capacity <= 0 || perSecond <= 0) { throw new IllegalArgumentException("Os limites devem ser positivos."); }
        this.capacity = capacity;
        this.perSecond = perSecond;
        this.tokens = capacity;
        this.lastNanos = now;
    }

    public boolean tryAcquire() {
        return tryAcquire(System.nanoTime());
    }

    public synchronized boolean tryAcquire(long now) {
        long elapsed = now - this.lastNanos;
        if (elapsed > 0) {
            this.tokens = Math.min(this.capacity, this.tokens + elapsed / 1_000_000_000d * this.perSecond);
            this.lastNanos = now;
        }
        if (this.tokens < 1) { return false; }
        --this.tokens;
        return true;
    }
}
