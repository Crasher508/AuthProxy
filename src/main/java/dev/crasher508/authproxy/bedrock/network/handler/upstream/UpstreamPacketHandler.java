/*
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package dev.crasher508.authproxy.bedrock.network.handler.upstream;

import com.google.gson.JsonParser;
import dev.crasher508.authproxy.AuthProxy;
import dev.crasher508.authproxy.bedrock.player.PlayerInfo;
import dev.crasher508.authproxy.bedrock.player.ProxiedPlayer;
import dev.crasher508.authproxy.bedrock.server.ProxyServer;
import dev.crasher508.authproxy.utils.*;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.protocol.bedrock.util.ChainValidationResult;
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.jws.JsonWebSignature;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;

public class UpstreamPacketHandler implements BedrockPacketHandler {

    protected final ProxiedPlayer player;

    public UpstreamPacketHandler(ProxiedPlayer player) {
        this.player = player;
    }

    @Override
    public PacketSignal handle(RequestNetworkSettingsPacket packet) {
        if (packet.getProtocolVersion() != ProxyServer.BEDROCK_CODEC.getProtocolVersion()) {
            PlayStatusPacket status = new PlayStatusPacket();
            status.setStatus((packet.getProtocolVersion() > ProxyServer.BEDROCK_CODEC.getProtocolVersion() ?
                    PlayStatusPacket.Status.LOGIN_FAILED_SERVER_OLD :
                    PlayStatusPacket.Status.LOGIN_FAILED_CLIENT_OLD));
            player.getSession().sendPacketImmediately(status);
            player.getSession().disconnect();
            Console.writeLn(TextFormat.RED + "[" + player.getSession().getSocketAddress() + "] <-> Upstream has disconnected due to incompatible protocol (protocol=" + packet.getProtocolVersion() + ")");
            return PacketSignal.HANDLED;
        }
        NetworkSettingsPacket networkSettingsPacket = new NetworkSettingsPacket();
        networkSettingsPacket.setCompressionThreshold(1);
        networkSettingsPacket.setCompressionAlgorithm(PacketCompressionAlgorithm.ZLIB);
        networkSettingsPacket.setClientThrottleEnabled(false);
        networkSettingsPacket.setClientThrottleThreshold(0);
        networkSettingsPacket.setClientThrottleScalar(0);
        player.getSession().setCodec(ProxyServer.BEDROCK_CODEC);
        player.getSession().sendPacketImmediately(networkSettingsPacket);
        player.getSession().getPeer().setCompression(PacketCompressionAlgorithm.ZLIB);
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(LoginPacket packet) {
        PlayerInfo playerInfo;
        try {
            ChainValidationResult chain = EncryptionUtils.validateChain(packet.getChain());
            String extraDataJson = chain.rawIdentityClaims().get("extraData").toString();
            if (extraDataJson == null) {
                throw new RuntimeException("AuthData was not found!");
            }

            String keyJson = chain.rawIdentityClaims().get("identityPublicKey").toString();
            if (keyJson == null) {
                throw new RuntimeException("AuthData was not found!");
            }
            ECPublicKey identityPublicKey = EncryptionUtils.parseKey(keyJson);

            String clientJwt = packet.getExtra();
            JsonWebSignature verifyJws = new JsonWebSignature();
            verifyJws.setKey(identityPublicKey);
            verifyJws.setCompactSerialization(clientJwt);
            boolean verified = verifyJws.verifySignature();
            try {
                JsonParser.parseString(extraDataJson).getAsJsonObject();
            } catch (Exception exception) {
                verified = false;
            }
            JsonWebSignature jws = new JsonWebSignature();
            jws.setCompactSerialization(clientJwt);
            JSONObject skinData = Json.parseJSONObject(jws.getUnverifiedPayload());
            playerInfo = new PlayerInfo(
                    chain.identityClaims().extraData.displayName,
                    chain.identityClaims().extraData.xuid,
                    chain.identityClaims().extraData.identity.toString()
            );
            player.setMainPlayerInfo(playerInfo);
            player.setSkinData(skinData);
            Console.writeLn(TextFormat.RED + "[" + player.getSession().getSocketAddress() + "|" + playerInfo.username() + "] <-> Upstream has connected. Account is " + (verified ? "Mojang Authed" : "Fake"));
            byte[] token = EncryptionUtils.generateRandomToken();
            KeyPair privateKeyPair = EncryptionUtils.createKeyPair();
            SecretKey encryptionKey = EncryptionUtils.getSecretKey(privateKeyPair.getPrivate(), identityPublicKey, token);

            ServerToClientHandshakePacket handshakePacket = new ServerToClientHandshakePacket();
            handshakePacket.setJwt(EncryptionUtils.createHandshakeJwt(privateKeyPair, token));

            player.getSession().getPeer().getChannel().eventLoop().execute(() -> {
                player.getSession().sendPacketImmediately(handshakePacket);
                player.getSession().enableEncryption(encryptionKey);
            });
        } catch (Exception exception) {
            player.getSession().disconnect("disconnectionScreen.internalError.cantConnect");
            Console.writeLn(TextFormat.RED + exception.getMessage());
            return PacketSignal.HANDLED;
        }
        if (AuthProxy.getDataProvider().getAccountByName(playerInfo.username()) == null) {
            player.getSession().disconnect("Du bist nicht angemeldet!\nBitte registriere dich unter dem Namen " + playerInfo.username());
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(ClientCacheStatusPacket packet) {
        if (player.isAllowSendPacket()) {
            player.sendDownstreamPacket(packet);
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(ClientToServerHandshakePacket packet) {
        this.finishConnection();
        return PacketSignal.HANDLED;
    }

    private void finishConnection() {
        PlayStatusPacket playStatusPacket = new PlayStatusPacket();
        playStatusPacket.setStatus(PlayStatusPacket.Status.LOGIN_SUCCESS);
        player.getSession().sendPacket(playStatusPacket);
        // Connect to downstream server
        String downstreamAddress = ProxyServer.getInstance().getDownstreamAddress();
        int downstreamPort = ProxyServer.getInstance().getDownstreamPort();
        player.connect(downstreamAddress, downstreamPort);
    }

    @Override
    public PacketSignal handle(SetLocalPlayerAsInitializedPacket packet) {
        player.sendMessage("§c[!] You are joined through Bedrock AuthProxy!");
        player.sendMessage("§a\n");
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(LevelChunkPacket packet) {
        packet.getData().retain();
        return PacketSignal.UNHANDLED;
    }

    @Override
    public PacketSignal handle(PacketViolationWarningPacket packet) {
        Console.writeLn(TextFormat.RED + "Packet violation warning: " + packet.getContext());
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(EmotePacket packet) {
        if (player.isAllowSendPacket()) {
            if (player.getPlayerInfo() != null)
                packet.setXuid(player.getPlayerInfo().xuid());
            player.sendDownstreamPacket(packet);
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public void onDisconnect(String reason) {
        Console.writeLn(TextFormat.RED + "Disconnect packet received from upstream server: " + reason);
        player.disconnect(reason);
    }
}
