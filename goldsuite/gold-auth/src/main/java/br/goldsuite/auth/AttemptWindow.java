package br.goldsuite.auth;

/** Janela por IP; a mesma instancia sobrevive a reconexoes do jogador. */
final class AttemptWindow {
    private long start;
    private int count;
    AttemptWindow(long now) { this.start = now; }
    boolean acquire(long now) {
        if (now - this.start >= 60_000_000_000L) { this.start = now; this.count = 0; }
        if (this.count >= 20) { return false; }
        ++this.count;
        return true;
    }
    boolean expired(long now) { return now - this.start >= 600_000_000_000L; }
}
