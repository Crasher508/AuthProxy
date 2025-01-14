/*
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package dev.crasher508.authproxy;

import com.google.gson.JsonObject;
import dev.crasher508.authproxy.account.AccountManager;
import dev.crasher508.authproxy.bedrock.server.ProxyServer;
import dev.crasher508.authproxy.utils.Console;
import dev.crasher508.authproxy.utils.FileManager;
import dev.crasher508.authproxy.utils.Json;
import dev.crasher508.authproxy.utils.TextFormat;
import dev.crasher508.authproxy.utils.threads.ConsoleThread;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.bedrock.session.StepFullBedrockSession;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;
import org.jose4j.json.internal.json_simple.JSONObject;

import java.io.File;
import java.net.InetSocketAddress;

public class AuthProxy {

    public static void main(String[] args) {
        long startTime = (System.currentTimeMillis() / 1000L);
        Console.writeLn("Starting Minecraft Bedrock AuthProxy...");

        File configFile = new File("config.json");
        if (!configFile.exists()) {
            String contents = FileManager.getFileResourceAsString("config.json");
            FileManager.writeToFile("config.json", contents);
        }

        JSONObject config = Json.parseJSONObject(FileManager.getFileContents("config.json"));

        JSONObject storageConfig = Json.parseJSONObjectFromObject(config, "storage");
        String secretKey = Json.parseStringFromJSONObject(storageConfig, "secret_key");
        String fileName = Json.parseStringFromJSONObject(storageConfig, "file_name");
        new AccountManager(secretKey, fileName);

        JSONObject proxyConfig = Json.parseJSONObjectFromObject(config, "proxy");
        String hostname = Json.parseStringFromJSONObject(proxyConfig, "hostname");
        long port = Json.parseLongFromJSONObject(proxyConfig, "port");
        JSONObject serverConfig = Json.parseJSONObjectFromObject(config, "server");
        String downstreamAddress = Json.parseStringFromJSONObject(serverConfig, "address");
        int downstreamPort = Math.round(Json.parseLongFromJSONObject(serverConfig, "port"));
        InetSocketAddress bindAddress = new InetSocketAddress(hostname, Math.round(port));
        ProxyServer proxyServer = new ProxyServer(bindAddress, downstreamAddress, downstreamPort);
        proxyServer.start();
        Console.writeLn("Proxy server Listening on " + bindAddress);

        Console.writeLn("Done (took " + (System.currentTimeMillis() / 1000L - startTime) + "s)!");

        ConsoleThread consoleThread = new ConsoleThread();
        consoleThread.start();
    }

    public static void startAuth() {
        Console.write(TextFormat.RED + "Name of upstream account: " + TextFormat.AQUA);
        String name = Console.read();
        HttpClient httpClient = MinecraftAuth.createHttpClient();
        try {
            StepFullBedrockSession.FullBedrockSession bedrockSession = MinecraftAuth.BEDROCK_DEVICE_CODE_LOGIN.getFromInput(httpClient, new StepMsaDeviceCode.MsaDeviceCodeCallback(msaDeviceCode -> {
                Console.writeLn(TextFormat.RED + "Visit: " + TextFormat.AQUA + msaDeviceCode.getVerificationUri() + TextFormat.RED + " Code: " + TextFormat.AQUA + msaDeviceCode.getUserCode());
                Console.writeLn(TextFormat.RED + "URL: " + TextFormat.AQUA + msaDeviceCode.getDirectVerificationUri());
            }));
            Console.writeLn("Username: " + bedrockSession.getMcChain().getDisplayName());
            Console.writeLn("Xuid: " + bedrockSession.getMcChain().getXuid());
            JsonObject bedrockSessionJsonObject = MinecraftAuth.BEDROCK_DEVICE_CODE_LOGIN.toJson(bedrockSession);
            if (!AccountManager.getInstance().saveAccount(name, bedrockSessionJsonObject)) {
                Console.writeLn(TextFormat.RED + "Failed to save account!");
            }
        } catch (Exception exception) {
            Console.writeLn(TextFormat.RED + "Failed to authenticate with your Microsoft account. " + exception.getMessage());
        }
    }
}