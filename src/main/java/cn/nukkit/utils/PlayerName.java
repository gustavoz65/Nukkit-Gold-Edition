package cn.nukkit.utils;

/** Nomes usados em arquivos e permissoes precisam ter uma representacao unica. */
public final class PlayerName {
    private PlayerName() { }

    public static boolean isValid(String name) {
        return name != null && name.matches("[A-Za-z0-9_]{3,16}")
                && !name.equalsIgnoreCase("console") && !name.equalsIgnoreCase("rcon")
                && !name.matches("(?i)(con|prn|aux|nul|com[1-9]|lpt[1-9])");
    }
}
