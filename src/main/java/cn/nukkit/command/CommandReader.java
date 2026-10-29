package cn.nukkit.command;

import cn.nukkit.InterruptibleThread;
import cn.nukkit.Nukkit;
import cn.nukkit.Server;
import cn.nukkit.event.server.ServerCommandEvent;
import cn.nukkit.timings.Timings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import jline.console.ConsoleReader;
import jline.console.CursorBuffer;

/** Leitor do console; baseado no leitor original de MagicDroidX/Nukkit Project. */
public class CommandReader extends Thread implements InterruptibleThread {
    private ConsoleReader reader;
    private final BufferedReader simpleReader;
    public static CommandReader instance;
    private CursorBuffer stashed;
    private volatile boolean running = true;

    public static CommandReader getInstance() {
        return instance;
    }

    public CommandReader() {
        if (instance != null) {
            throw new IllegalStateException("O leitor de comandos ja foi criado.");
        }
        instance = this;
        this.simpleReader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        if (Nukkit.ANSI) {
            try {
                this.reader = new ConsoleReader(System.in, System.out);
                this.reader.setPrompt("> ");
            } catch (IOException | RuntimeException | LinkageError e) {
                Nukkit.ANSI = false;
                System.err.println("Terminal avancado indisponivel; usando o console simples em UTF-8.");
            }
        }
        this.setName("Console");
    }

    public String readLine() {
        try {
            return this.reader == null ? this.simpleReader.readLine() : this.reader.readLine("> ");
        } catch (IOException e) {
            Server.getInstance().getLogger().error("Nao foi possivel ler o console.", e);
            return null;
        }
    }

    @Override
    public void run() {
        while (this.running) {
            if (Server.getInstance().getConsoleSender() == null || Server.getInstance().getPluginManager() == null) {
                try {
                    Thread.sleep(40);
                } catch (InterruptedException e) {
                    return;
                }
                continue;
            }
            String line = readLine();
            // Entrada encerrada nao deve criar um loop consumindo CPU.
            if (line == null) {
                return;
            }
            if (!line.trim().isEmpty()) {
                // Ler stdin nao deve executar comandos de mundo na thread do console.
                Server.getInstance().getScheduler().scheduleTask(() -> {
                    Timings.serverCommandTimer.startTiming();
                    try {
                        ServerCommandEvent event = new ServerCommandEvent(Server.getInstance().getConsoleSender(), line);
                        Server.getInstance().getPluginManager().callEvent(event);
                        if (!event.isCancelled()) {
                            Server.getInstance().dispatchCommand(event.getSender(), event.getCommand());
                        }
                    } catch (Exception e) {
                        Server.getInstance().getLogger().logException(e);
                    } finally {
                        Timings.serverCommandTimer.stopTiming();
                    }
                });
            }
        }
    }

    public void shutdown() {
        this.running = false;
    }

    public synchronized void stashLine() {
        if (this.reader == null || !Nukkit.ANSI) {
            return;
        }
        this.stashed = this.reader.getCursorBuffer().copy();
        try {
            this.reader.getOutput().write("\u001b[1G\u001b[K");
            this.reader.flush();
        } catch (IOException ignored) {
            // A falha de redesenho nao impede o registro da mensagem.
        }
    }

    public synchronized void unstashLine() {
        if (this.reader == null || this.stashed == null || !Nukkit.ANSI) {
            return;
        }
        try {
            this.reader.resetPromptLine("> ", this.stashed.toString(), this.stashed.cursor);
        } catch (IOException ignored) {
            // A falha de redesenho nao interrompe o servidor.
        }
    }

    public void removePromptLine() {
        if (this.reader == null || !Nukkit.ANSI) {
            return;
        }
        try {
            this.reader.resetPromptLine("", "", 0);
        } catch (IOException ignored) {
            // O console pode ter sido fechado durante o desligamento.
        }
    }
}
