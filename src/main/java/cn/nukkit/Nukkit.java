package cn.nukkit;

import cn.nukkit.command.CommandReader;
import cn.nukkit.utils.MainLogger;
import cn.nukkit.utils.ServerKiller;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

/**
 * `_   _       _    _    _ _
 * | \ | |     | |  | |  (_) |
 * |  \| |_   _| | _| | ___| |_
 * | . ` | | | | |/ / |/ / | __|
 * | |\  | |_| |   <|   <| | |_
 * |_| \_|\__,_|_|\_\_|\_\_|\__|
 */

/**
 * Nukkit启动类，包含{@code main}函数。<br>
 * The launcher class of Nukkit, including the {@code main} function.
 *
 * @author MagicDroidX(code) @ Nukkit Project
 * @author 粉鞋大妈(javadoc) @ Nukkit Project
 * @since Nukkit 1.0 | Nukkit API 1.0.0
 */
public class Nukkit {

    public final static String NAME = "Nukkit-Gold";
    public final static String VERSION = "0.2.0";
    public final static String API_VERSION = "1.0.0";
    public final static String CODENAME = "Gold";
    public final static String MINECRAFT_VERSION = "v0.15.10 alpha";
    public final static String MINECRAFT_VERSION_NETWORK = "0.15.10";

    public final static String PATH = System.getProperty("user.dir") + "/";
    public final static String DATA_PATH = System.getProperty("user.dir") + "/";
    public final static String PLUGIN_PATH = DATA_PATH + "plugins";
    public static final long START_TIME = System.currentTimeMillis();
    public static volatile boolean ANSI = true;
    public static boolean shortTitle = false;
    public static int DEBUG = 1;

    public static void main(String[] args) {
        // file.encoding sozinho nao muda um stream que a JVM ja criou.
        try {
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, "UTF-8"));
            System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("A JVM precisa oferecer suporte a UTF-8.", e);
        }

        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        // O console simples dispensa DLLs no Windows e em entrada redirecionada.
        if (osName.contains("windows") || System.console() == null) {
            ANSI = false;
        }
        if (osName.contains("windows")) {
            if (osName.contains("windows 8") || osName.contains("2012")) {
                shortTitle = true;
            }
        }

        // Opcoes do iniciador.
        for (String arg : args) {
            switch (arg) {
                case "disable-ansi":
                    ANSI = false;
                    break;
            }
        }

        MainLogger logger = new MainLogger(DATA_PATH + "server.log");

        try {
            if (ANSI) {
                System.out.print((char) 0x1b + "]0;Iniciando " + NAME + (char) 0x07);
            }
            Server server = new Server(logger, PATH, DATA_PATH, PLUGIN_PATH);
        } catch (Exception e) {
            logger.logException(e);
        }

        if (ANSI) {
            System.out.print((char) 0x1b + "]0;Encerrando servidor..." + (char) 0x07);
        }
        logger.info("Encerrando as outras threads");

        for (Thread thread : java.lang.Thread.getAllStackTraces().keySet()) {
            if (!(thread instanceof InterruptibleThread)) {
                continue;
            }
            logger.debug("Encerrando thread " + thread.getClass().getSimpleName());
            if (thread.isAlive()) {
                thread.interrupt();
            }
        }

        ServerKiller killer = new ServerKiller(8);
        killer.start();

        logger.shutdown();
        logger.interrupt();
        if (CommandReader.getInstance() != null) {
            CommandReader.getInstance().removePromptLine();
        }

        if (ANSI) {
            System.out.print((char) 0x1b + "]0;Servidor encerrado" + (char) 0x07);
        }
        System.exit(0);
    }


}
