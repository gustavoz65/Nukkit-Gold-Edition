package cn.nukkit.command.defaults;

import cn.nukkit.command.CommandSender;
import cn.nukkit.raknet.server.Shield;

import java.util.Map;

/** /antiddos [status|lista|desbloquear <ip>]: estado do anti-DDoS do RakNet (Nukkit Gold). So OP/console. */
public class AntiDdosCommand extends VanillaCommand {

    public AntiDdosCommand(String name) {
        super(name, "Estado do anti-DDoS e IPs bloqueados", "/antiddos [status|lista|desbloquear <ip>]");
        this.setPermission("nukkit.command.antiddos");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }
        Shield shield = Shield.current;
        if (shield == null) {
            sender.sendMessage("Anti-DDoS ainda nao iniciado.");
            return true;
        }
        String sub = args.length == 0 ? "status" : args[0].toLowerCase();
        switch (sub) {
            case "lista": {
                long now = System.currentTimeMillis();
                if (shield.blocked.isEmpty()) { sender.sendMessage("Nenhum IP bloqueado."); return true; }
                int shown = 0;
                for (Map.Entry<String, Long> e : shield.blocked.entrySet()) {
                    if (shown++ >= 30) { sender.sendMessage("... e mais " + (shield.blocked.size() - 30)); break; }
                    long left = e.getValue() == Long.MAX_VALUE ? -1 : Math.max(0, (e.getValue() - now) / 1000);
                    sender.sendMessage(e.getKey() + " - " + (left < 0 ? "permanente" : left + " s"));
                }
                return true;
            }
            case "desbloquear": {
                if (args.length < 2) { sender.sendMessage("Uso: /antiddos desbloquear <ip>"); return true; }
                sender.sendMessage(shield.blocked.remove(args[1]) != null ? args[1] + " desbloqueado." : args[1] + " nao estava bloqueado.");
                return true;
            }
            default:
                sender.sendMessage("Anti-DDoS: " + shield.status());
                sender.sendMessage("Limites por IP: " + shield.packetsPerSecond + " pacotes/s, " + shield.pingsPerSecond + " pings/s, " + shield.sessionsPerIp + " conexoes abertas, "
                        + shield.newSessionsPerMinute + " novas/min; modo ataque acima de " + shield.attackNewPerSecond + " conexoes/s (deixa entrar " + shield.attackAllowPerSecond + "/s).");
                return true;
        }
    }
}
