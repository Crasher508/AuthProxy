package dev.crasher508.authproxy.utils.threads;

import com.google.gson.JsonObject;
import dev.crasher508.authproxy.AuthProxy;
import dev.crasher508.authproxy.bedrock.network.session.ProxyServerSession;
import dev.crasher508.authproxy.utils.Console;
import dev.crasher508.authproxy.utils.TextFormat;
import lombok.Getter;
import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.bedrock.session.StepFullBedrockSession;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;

public class PlayerAuthThread extends Thread {

    private final String playerName;
    private final ProxyServerSession session;

    @Getter
    private StepMsaDeviceCode.MsaDeviceCode msaDeviceCode = null;

    public PlayerAuthThread(String playerName, ProxyServerSession session) {
        this.playerName = playerName;
        this.session = session;
    }

    @Override
    public void run() {
        HttpClient httpClient = MinecraftAuth.createHttpClient();
        try {
            StepFullBedrockSession.FullBedrockSession bedrockSession = MinecraftAuth.BEDROCK_DEVICE_CODE_LOGIN.getFromInput(httpClient, new StepMsaDeviceCode.MsaDeviceCodeCallback(msaDeviceCode -> {
                this.msaDeviceCode = msaDeviceCode;
                this.session.disconnect("§lNote§bLand\n§r§cDu bist nicht angemeldet!\nBitte registriere dich unter: " + msaDeviceCode.getVerificationUri() + " Code: " + msaDeviceCode.getUserCode());
            }));
            Console.writeLn("Username: " + bedrockSession.getMcChain().getDisplayName());
            Console.writeLn("Xuid: " + bedrockSession.getMcChain().getXuid());
            JsonObject bedrockSessionJsonObject = MinecraftAuth.BEDROCK_DEVICE_CODE_LOGIN.toJson(bedrockSession);
            if (!AuthProxy.getDataProvider().saveAccount(this.playerName, bedrockSessionJsonObject)) {
                Console.writeLn(TextFormat.RED + "Failed to save account!");
            }
        } catch (Exception exception) {
            Console.writeLn(TextFormat.RED + "Failed to authenticate with Microsoft account. " + exception.getMessage());
        }
        AuthProxy.getAuthThreads().remove(this.playerName);
    }
}
