package cn.nukkit.raknet.server;

import cn.nukkit.Server;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Anti-DDoS de aplicacao do Nukkit Gold, na thread do RakNet (antes de qualquer decodificacao do jogo).
 * Limita pacotes por segundo, pings da lista de servidores, sessoes abertas e novas conexoes por IP, e entra em
 * "modo ataque" (fila de entrada) quando surgem conexoes demais no servidor todo. Floods volumetricos que lotam o
 * link precisam de protecao na rede (hospedagem/firewall); isto cuida do que chega ate o Java.
 * Configuracao em nukkit.yml, secao network.anti-ddos.
 */
public final class Shield {
    /** Instancia ativa, para o comando /antiddos (outra thread). */
    public static volatile Shield current;

    public final boolean enabled;
    public final int packetsPerSecond, pingsPerSecond, sessionsPerIp, newSessionsPerMinute, attackNewPerSecond, attackAllowPerSecond, blockSeconds;
    /** IP -> instante em que o bloqueio acaba. Compartilhado com o SessionManager e o comando. */
    public final Map<String, Long> blocked = new ConcurrentHashMap<>();

    private final Map<String, int[]> second = new HashMap<>();
    private final Map<String, ArrayDeque<Long>> recentSessions = new HashMap<>();
    private long window;
    private int globalNew, attackAccepted;
    private volatile long attackUntil;
    private volatile long droppedPackets, droppedPings, refusedSessions;

    public Shield(boolean enabled, int packetsPerSecond, int pingsPerSecond, int sessionsPerIp, int newSessionsPerMinute, int attackNewPerSecond, int attackAllowPerSecond, int blockSeconds) {
        this.enabled = enabled;
        this.packetsPerSecond = packetsPerSecond; this.pingsPerSecond = pingsPerSecond; this.sessionsPerIp = sessionsPerIp;
        this.newSessionsPerMinute = newSessionsPerMinute; this.attackNewPerSecond = attackNewPerSecond; this.attackAllowPerSecond = attackAllowPerSecond;
        this.blockSeconds = blockSeconds;
    }

    /** Le network.anti-ddos do nukkit.yml (ou usa os padroes). */
    public static Shield fromConfig() {
        Server server = Server.getInstance();
        return new Shield(bool(server, "enabled", true), num(server, "packets-per-second", 1500), num(server, "pings-per-second", 5),
                num(server, "sessions-per-ip", 5), num(server, "new-sessions-per-minute", 20), num(server, "attack-new-sessions-per-second", 25),
                num(server, "attack-allow-per-second", 3), num(server, "block-seconds", 300));
    }

    private static int num(Server server, String key, int def) {
        if (server == null) { return def; }
        Object v = server.getConfig("network.anti-ddos." + key, def);
        try { return Math.max(1, Integer.parseInt(String.valueOf(v))); } catch (NumberFormatException e) { return def; }
    }

    private static boolean bool(Server server, String key, boolean def) {
        if (server == null) { return def; }
        Object v = server.getConfig("network.anti-ddos." + key, def);
        return v instanceof Boolean ? (Boolean) v : Boolean.parseBoolean(String.valueOf(v));
    }

    private void roll(long now) {
        long w = now / 1000;
        if (w != window) { window = w; second.clear(); globalNew = 0; attackAccepted = 0; }
    }

    /** Conta o pacote; false = IP passou do limite e deve ser bloqueado. */
    public boolean packet(String ip, long now) {
        if (!enabled) { return true; }
        roll(now);
        int[] c = second.computeIfAbsent(ip, k -> new int[2]);
        if (++c[0] > packetsPerSecond) { droppedPackets++; return false; }
        return true;
    }

    /** Ping da lista de servidores: o excesso e descartado em silencio (sem resposta, sem amplificacao). */
    public boolean ping(String ip, long now) {
        if (!enabled) { return true; }
        roll(now);
        int[] c = second.computeIfAbsent(ip, k -> new int[2]);
        if (++c[1] > pingsPerSecond) { droppedPings++; return false; }
        return true;
    }

    /**
     * Nova sessao (ip:porta inedito). Recusa se o IP ja tem sessoes demais, abriu conexoes demais no ultimo minuto,
     * ou se o servidor esta em modo ataque e a cota do segundo acabou. Retorna null se aceita, ou o motivo.
     */
    public String newSession(String ip, int openForIp, long now) {
        if (!enabled) { return null; }
        roll(now);
        if (openForIp >= sessionsPerIp) { refusedSessions++; return "sessoes demais do mesmo IP"; }
        ArrayDeque<Long> times = recentSessions.computeIfAbsent(ip, k -> new ArrayDeque<>());
        while (!times.isEmpty() && now - times.peekFirst() > 60_000) { times.pollFirst(); }
        if (times.size() >= newSessionsPerMinute) { refusedSessions++; return "conexoes demais por minuto"; }
        if (++globalNew > attackNewPerSecond && attackUntil < now) { attackUntil = now + 30_000; }
        if (attackUntil > now && ++attackAccepted > attackAllowPerSecond) { refusedSessions++; return "modo ataque"; }
        times.addLast(now);
        if (recentSessions.size() > 50_000) { recentSessions.clear(); }
        return null;
    }

    public boolean underAttack() { return attackUntil > System.currentTimeMillis(); }

    public boolean isBlocked(String ip, long now) {
        Long until = blocked.get(ip);
        if (until == null) { return false; }
        if (until <= now) { blocked.remove(ip); return false; }
        return true;
    }

    /** Remove bloqueios vencidos (todos, nao so os primeiros). */
    public List<String> expire(long now) {
        List<String> freed = new ArrayList<>();
        for (Iterator<Map.Entry<String, Long>> it = blocked.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Long> e = it.next();
            if (e.getValue() <= now) { freed.add(e.getKey()); it.remove(); }
        }
        return freed;
    }

    public String status() {
        return (enabled ? "ligado" : "desligado") + (underAttack() ? " | MODO ATAQUE (fila de entrada)" : "") + " | bloqueados: " + blocked.size()
                + " | pacotes descartados: " + droppedPackets + " | pings descartados: " + droppedPings + " | conexoes recusadas: " + refusedSessions;
    }
}
