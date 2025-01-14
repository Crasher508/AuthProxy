package dev.crasher508.authproxy.utils;

import java.util.Scanner;

public class Console {

    public static void write(String string) {
        System.out.print(TextFormat.replace(string));
    }

    public static void writeLn(String string) {
        System.out.println(TextFormat.replace(string));
    }

    public static String read() {
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine();
    }
}