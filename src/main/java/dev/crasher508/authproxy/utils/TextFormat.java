package dev.crasher508.authproxy.utils;

import java.util.HashMap;

public class TextFormat {

    private static final String ESCAPE = "§";

    public static final String BLACK = TextFormat.ESCAPE + "0";
    public static final String DARK_BLUE = TextFormat.ESCAPE + "1";
    public static final String DARK_GREEN = TextFormat.ESCAPE + "2";
    public static final String DARK_AQUA = TextFormat.ESCAPE + "3";
    public static final String DARK_RED = TextFormat.ESCAPE + "4";
    public static final String DARK_PURPLE = TextFormat.ESCAPE + "5";
    public static final String GOLD = TextFormat.ESCAPE + "6";
    public static final String GRAY = TextFormat.ESCAPE + "7";
    public static final String DARK_GRAY = TextFormat.ESCAPE + "8";
    public static final String BLUE = TextFormat.ESCAPE + "9";
    public static final String GREEN = TextFormat.ESCAPE + "a";
    public static final String AQUA = TextFormat.ESCAPE + "b";
    public static final String RED = TextFormat.ESCAPE + "c";
    public static final String LIGHT_PURPLE = TextFormat.ESCAPE + "d";
    public static final String YELLOW = TextFormat.ESCAPE + "e";
    public static final String WHITE = TextFormat.ESCAPE + "f";
    public static final String MINECOIN_GOLD = TextFormat.ESCAPE + "g";

    private static final String ANSI_RESET = "\u001B[m";

    private static HashMap<String, String> colors;

    public static String replace(String string) {
        if (colors == null) {
            colors = new HashMap<>();
            colors.put(TextFormat.BLACK, "\u001B[0;30m");
            colors.put(TextFormat.DARK_BLUE, "\u001B[0;34m");
            colors.put(TextFormat.DARK_GREEN, "\u001B[0;32m");
            colors.put(TextFormat.DARK_AQUA, "\u001B[0;36m");
            colors.put(TextFormat.DARK_RED, "\u001B[0;31m");
            colors.put(TextFormat.DARK_PURPLE, "\u001B[0;35m");
            colors.put(TextFormat.GOLD, "\u001B[0;33m");
            colors.put(TextFormat.GRAY, "\u001B[0;37m");
            colors.put(TextFormat.DARK_GRAY, "\u001B[0;30;1m");
            colors.put(TextFormat.BLUE, "\u001B[0;34;1m");
            colors.put(TextFormat.GREEN, "\u001B[0;32;1m");
            colors.put(TextFormat.AQUA, "\u001B[0;36;1m");
            colors.put(TextFormat.RED, "\u001B[0;31;1m");
            colors.put(TextFormat.LIGHT_PURPLE, "\u001B[0;35;1m");
            colors.put(TextFormat.YELLOW, "\u001B[0;33;1m");
            colors.put(TextFormat.WHITE, "\u001B[0;37;1m");
            colors.put("§k", "\u001B[5m");
            colors.put("§l", "\u001B[21m");
            colors.put("§m", "\u001B[9m");
            colors.put("§n", "\u001B[4m");
            colors.put("§o", "\u001B[3m");
            colors.put("§r", TextFormat.ANSI_RESET);
        }
        for(HashMap.Entry<String, String> entry : colors.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            string = string.replaceAll(key, value);
        }
        return string + ANSI_RESET;
    }
}
