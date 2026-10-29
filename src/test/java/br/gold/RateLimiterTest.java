package br.gold;

import cn.nukkit.utils.RateLimiter;

public final class RateLimiterTest {
    public static void main(String[] args) {
        RateLimiter limitador = new RateLimiter(4, 4, 0);
        for (int i = 0; i < 4; i++) {
            if (!limitador.tryAcquire(0)) { throw new AssertionError("Rajada inicial foi recusada."); }
        }
        if (limitador.tryAcquire(0) || limitador.tryAcquire(249_999_999L)) {
            throw new AssertionError("Flood foi aceito sem reposicao suficiente.");
        }
        if (!limitador.tryAcquire(250_000_000L) || limitador.tryAcquire(250_000_000L)) {
            throw new AssertionError("Reposicao gradual incorreta.");
        }
        if (limitador.tryAcquire(0)) { throw new AssertionError("Recuo do relogio liberou mensagens."); }
        for (int i = 0; i < 4; i++) {
            if (!limitador.tryAcquire(10_000_000_000L)) { throw new AssertionError("Intervalo nao recuperou a capacidade."); }
        }
        if (limitador.tryAcquire(10_000_000_000L)) { throw new AssertionError("Acumulou tokens acima da capacidade."); }
        System.out.println("RateLimiterTest: rajada, flood, reposicao e limite maximo verificados.");
    }
}
