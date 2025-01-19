/*
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package dev.crasher508.authproxy.bedrock.network.handler.downstream;

import com.google.gson.JsonObject;
import dev.crasher508.authproxy.AuthProxy;
import dev.crasher508.authproxy.bedrock.network.registry.FakeDefinitionRegistry;
import dev.crasher508.authproxy.bedrock.player.ProxiedPlayer;
import dev.crasher508.authproxy.bedrock.server.ProxyServer;
import dev.crasher508.authproxy.utils.Console;
import dev.crasher508.authproxy.utils.TextFormat;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.SneakyThrows;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.bedrock.session.StepFullBedrockSession;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodecHelper;
import org.cloudburstmc.protocol.bedrock.data.EncodingSettings;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;
import org.cloudburstmc.protocol.bedrock.util.JsonUtils;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.cloudburstmc.protocol.common.SimpleDefinitionRegistry;
import org.jose4j.json.JsonUtil;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwx.HeaderParameterNames;
import org.jose4j.lang.JoseException;

import javax.crypto.SecretKey;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

public class DownstreamPacketHandler implements BedrockPacketHandler {

    protected final ProxiedPlayer player;

    public DownstreamPacketHandler(ProxiedPlayer player) {
        this.player = player;
    }

    @SneakyThrows
    @Override
    public PacketSignal handle(NetworkSettingsPacket packet) {
        player.getDownstreamSession().getPeer().setCodec(ProxyServer.BEDROCK_CODEC);
        player.getDownstreamSession().getPeer().setCompression(packet.getCompressionAlgorithm());
        player.getDownstreamSession().getPeer().getCodecHelper().setEncodingSettings(EncodingSettings.UNLIMITED);

        if (player.getMainPlayerInfo() == null) {
            player.getSession().disconnect("Etwas ist während deiner Autorisierung schief gelaufen!\nBitte versuche es später erneut.");
            return PacketSignal.HANDLED;
        }

        String javaPlayerName = player.getMainPlayerInfo().username();
        try {
            player.getDownstreamSession().sendPacketImmediately(player.getLoginPacket());
        } catch (Exception ignored) {
            Console.writeLn(TextFormat.RED + "Failed to send packet immediately: Try to refresh bedrock session of " + javaPlayerName);
            JsonObject bedrockSessionJsonObject = AuthProxy.getDataProvider().getAccountByName(javaPlayerName);
            StepFullBedrockSession.FullBedrockSession bedrockSession = MinecraftAuth.BEDROCK_DEVICE_CODE_LOGIN.fromJson(bedrockSessionJsonObject);
            StepFullBedrockSession.FullBedrockSession refreshedBedrockSession = MinecraftAuth.BEDROCK_DEVICE_CODE_LOGIN.refresh(MinecraftAuth.createHttpClient(), bedrockSession);
            AuthProxy.getDataProvider().saveAccount(javaPlayerName, MinecraftAuth.BEDROCK_DEVICE_CODE_LOGIN.toJson(refreshedBedrockSession));
            player.getDownstreamSession().sendPacketImmediately(player.getLoginPacket());
        }

        player.setAllowSendPacket(true);
        return PacketSignal.HANDLED;
    }

    @SneakyThrows
    @Override
    public PacketSignal handle(ServerToClientHandshakePacket packet) {
        try {
            JsonWebSignature jws = new JsonWebSignature();
            jws.setCompactSerialization(packet.getJwt());
            JSONObject saltJwt = new JSONObject(JsonUtil.parseJson(jws.getUnverifiedPayload()));
            String x5u = jws.getHeader(HeaderParameterNames.X509_URL);
            ECPublicKey serverKey = EncryptionUtils.parseKey(x5u);
            SecretKey key = EncryptionUtils.getSecretKey(
                    this.player.getKeyPair().getPrivate(),
                    serverKey,
                    Base64.getDecoder().decode(JsonUtils.childAsType(saltJwt, "salt", String.class))
            );
            this.player.getDownstreamSession().enableEncryption(key);
        } catch (JoseException | NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException exception) {
            throw new RuntimeException(exception);
        }
        player.getDownstreamSession().sendPacketImmediately(new ClientToServerHandshakePacket());
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(StartGamePacket packet) {
        FakeDefinitionRegistry<BlockDefinition> fakeDefinitionRegistry = FakeDefinitionRegistry.createBlockRegistry();
        SetPlayerGameTypePacket setPlayerGameTypePacket = new SetPlayerGameTypePacket();
        setPlayerGameTypePacket.setGamemode(packet.getPlayerGameType().ordinal());

        BedrockCodecHelper upstreamCodecHelper = player.getSession().getPeer().getCodecHelper();
        BedrockCodecHelper downstreamCodecHelper = player.getDownstreamSession().getPeer().getCodecHelper();
        SimpleDefinitionRegistry.Builder<ItemDefinition> itemRegistry = SimpleDefinitionRegistry.builder();
        IntSet runtimeIds = new IntOpenHashSet();
        for (ItemDefinition definition : packet.getItemDefinitions()) {
            if (runtimeIds.add(definition.getRuntimeId())) {
                itemRegistry.add(definition);
            }
        }
        upstreamCodecHelper.setItemDefinitions(itemRegistry.build());
        downstreamCodecHelper.setItemDefinitions(itemRegistry.build());
        upstreamCodecHelper.setBlockDefinitions(fakeDefinitionRegistry);
        downstreamCodecHelper.setBlockDefinitions(fakeDefinitionRegistry);

        player.setEntityRuntimeId(packet.getRuntimeEntityId());
        player.getSession().sendPacket(packet);
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(LevelChunkPacket packet) {
        packet.getData().retain();
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(DisconnectPacket packet) {
        Console.writeLn(TextFormat.RED + "Disconnected from downstream server: " + packet.getKickMessage());
        player.disconnect(packet.getKickMessage());
        return PacketSignal.HANDLED;
    }

    @Override
    public void onDisconnect(String reason) {
        Console.writeLn(TextFormat.RED + "Disconnected from downstream server: " + reason);
        player.disconnect(reason);
    }
}
