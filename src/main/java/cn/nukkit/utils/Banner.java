package cn.nukkit.utils;

import cn.nukkit.Nukkit;

/** Letreiro do Nukkit Gold mostrado ao iniciar: o bloco de ouro ao lado do nome em amarelo. So ASCII (funciona em qualquer console). */
public final class Banner {
    private Banner() {}

    private static final String G = TextFormat.GOLD.toString(), Y = TextFormat.YELLOW.toString(), W = TextFormat.WHITE.toString(), D = TextFormat.DARK_GRAY.toString(), R = TextFormat.RESET.toString();

    /** Bloco de ouro em perspectiva: tampa clara, frente dourada. */
    private static final String[] BLOCK = {
            Y + "    .--------. ",
            Y + "   /" + W + " :::::: " + Y + "/| ",
            Y + "  /________/" + G + " | ",
            G + "  | " + Y + "######" + G + " | | ",
            G + "  | " + Y + "##" + G + "  " + Y + "##" + G + " | / ",
            G + "  |________|/  ",
    };

    private static final String[] NAME = {
            " _   _ _   _ _  ___  _____ _____    ____  ___  _     ____  ",
            "| \\ | | | | | |/ / |/ /_ _|_   _|  / ___|/ _ \\| |   |  _ \\ ",
            "|  \\| | | | | ' /| ' / | |  | |   | |  _| | | | |   | | | |",
            "| |\\  | |_| | . \\| . \\ | |  | |   | |_| | |_| | |___| |_| |",
            "|_| \\_|\\___/|_|\\_\\_|\\_\\___| |_|    \\____|\\___/|_____|____/ ",
    };

    public static String[] lines() {
        String[] out = new String[BLOCK.length + 3];
        out[0] = "";
        for (int i = 0; i < BLOCK.length; i++) {
            String name = i == 0 ? "" : NAME[i - 1];
            out[i + 1] = BLOCK[i] + R + "  " + TextFormat.BOLD + Y + name + R;
        }
        out[BLOCK.length + 1] = D + "  ------------------------------------------------------------------------------" + R;
        out[BLOCK.length + 2] = "  " + Y + "Nukkit Gold " + W + Nukkit.VERSION + D + "  |  " + W + "Minecraft PE " + Nukkit.MINECRAFT_VERSION_NETWORK + D + "  |  " + G + "anti-DDoS ativo" + R + "\n";
        return out;
    }

    public static void print(MainLogger logger) {
        for (String line : lines()) {
            logger.raw(line);
        }
    }
}
