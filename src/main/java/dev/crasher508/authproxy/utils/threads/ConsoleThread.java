package dev.crasher508.authproxy.utils.threads;

import dev.crasher508.authproxy.AuthProxy;
import dev.crasher508.authproxy.bedrock.server.ProxyServer;
import dev.crasher508.authproxy.utils.Console;

public class ConsoleThread extends Thread {

    @Override
    public void run() {
        do {
            String input = Console.read();
            if (input.isEmpty())
                continue;
            if (input.startsWith("/")) {
                input = input.substring(1);
                String[] args = input.split(" ");
                if (args.length >= 1) {
                    String command = args[0];
                    switch (command) {
                        case "end" -> {
                            ProxyServer.getInstance().setRunning(false);
                            break;
                        }
                        case "auth" -> {
                            AuthProxy.startAuth();
                            break;
                        }
                    }
                }
                continue;
            }
        } while (ProxyServer.getInstance().isRunning());
    }
}
