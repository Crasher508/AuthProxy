package dev.crasher508.authproxy.bedrock.player;

import com.google.gson.*;
import dev.crasher508.authproxy.account.AccountManager;
import dev.crasher508.authproxy.bedrock.network.handler.downstream.DownstreamPacketHandler;
import dev.crasher508.authproxy.bedrock.network.session.ProxyClientSession;
import dev.crasher508.authproxy.bedrock.network.session.ProxyServerSession;
import dev.crasher508.authproxy.bedrock.server.ProxyServer;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.gson.io.GsonDeserializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.bedrock.session.StepFullBedrockSession;
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.netty.initializer.BedrockChannelInitializer;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.jose4j.json.internal.json_simple.JSONObject;

import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static net.raphimc.minecraftauth.step.bedrock.StepMCChain.MOJANG_PUBLIC_KEY;
import static net.raphimc.minecraftauth.util.CryptUtil.EC_KEYFACTORY;

@Getter
public class ProxiedPlayer {

    private final ProxyServerSession session;
    private ProxyClientSession downstreamSession = null;

    @Setter
    private String accessToken;
    private final String refreshToken;
    private KeyPair keyPair;

    @Setter
    private boolean allowSendPacket = false;

    private PlayerInfo playerInfo = null;
    @Setter
    private PlayerInfo mainPlayerInfo = null;
    @Setter
    private JSONObject skinData;
    @Setter
    private long entityRuntimeId;

    @SneakyThrows
    public ProxiedPlayer(ProxyServerSession session) {
        this.accessToken = this.refreshToken = "";
        this.session = session;
    }

    /**
     * Connect to the downstream server
     * @param address the address of the downstream server
     * @param port the port of the downstream server
     */
    public void connect(String address, int port) {
        ProxiedPlayer player = this;

        new Bootstrap()
                .channelFactory(RakChannelFactory.client(NioDatagramChannel.class))
                .group(new NioEventLoopGroup())
                .option(RakChannelOption.RAK_PROTOCOL_VERSION, ProxyServer.BEDROCK_CODEC.getRaknetProtocolVersion())
                .option(RakChannelOption.RAK_ORDERING_CHANNELS, 1)
                .option(RakChannelOption.RAK_SESSION_TIMEOUT, 10000L)
                .handler(new BedrockChannelInitializer<ProxyClientSession>() {

                    @Override
                    protected ProxyClientSession createSession0(BedrockPeer peer, int subClientId) {
                        return new ProxyClientSession(peer, subClientId);
                    }

                    @Override
                    protected void initSession(ProxyClientSession session) {
                        session.setCodec(ProxyServer.BEDROCK_CODEC);
                        RequestNetworkSettingsPacket pk = new RequestNetworkSettingsPacket();
                        pk.setProtocolVersion(ProxyServer.BEDROCK_CODEC.getProtocolVersion());
                        session.sendPacketImmediately(pk);
                        session.setPacketHandler(new DownstreamPacketHandler(player));
                        session.setSendSession(ProxiedPlayer.this.session);
                        ProxiedPlayer.this.downstreamSession = session;
                        ProxiedPlayer.this.session.setSendSession(ProxiedPlayer.this.downstreamSession);
                    }
                })
                .connect(new InetSocketAddress(address, port))
                .syncUninterruptibly();
    }

    /**
     * Send a packet to the downstream server
     *
     * @param packet the packet to send
     */
    public void sendDownstreamPacket(BedrockPacket packet) {
        ProxyClientSession session = this.downstreamSession;
        if (session != null) {
            session.sendPacket(packet);
        }
    }

    public void disconnect(String reason) {
        if (this.session.isConnected()) {
            this.session.disconnect(reason);
        }
        if (this.downstreamSession != null && this.downstreamSession.isConnected()) {
            this.downstreamSession.disconnect(reason);
        }
    }

    public void sendMessage(String message) {
        TextPacket pk = new TextPacket();
        pk.setType(TextPacket.Type.RAW);
        pk.setPlatformChatId("");
        pk.setXuid("");
        pk.setSourceName("");
        pk.setNeedsTranslation(false);
        pk.setMessage(message);
        this.session.sendPacket(pk);
    }

    private static final Gson GSON = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).disableHtmlEscaping().create();
    private static final GsonDeserializer<Map<String, ?>> GSON_DESERIALIZER = new GsonDeserializer<>(GSON);

    private static ECPublicKey publicKeyFromBase64(final String base64) {
        try {
            return (ECPublicKey) EC_KEYFACTORY.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(base64)));
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Could not decode base64 public key", e);
        }
    }

    public LoginPacket getLoginPacket() {
        LoginPacket loginPacket = new LoginPacket();
        String javaPlayerName = mainPlayerInfo.username();
        JsonObject bedrockSessionJsonObject = AccountManager.getInstance().getAccountByName(javaPlayerName);
        StepFullBedrockSession.FullBedrockSession bedrockSession = MinecraftAuth.BEDROCK_DEVICE_CODE_LOGIN.fromJson(bedrockSessionJsonObject);

        this.keyPair = new KeyPair(bedrockSession.getMcChain().getPublicKey(), bedrockSession.getMcChain().getPrivateKey());
        final String encodedPublicKey = Base64.getEncoder().encodeToString(bedrockSession.getMcChain().getPublicKey().getEncoded());

        final Jws<Claims> mojangJwt = Jwts.parser().clockSkewSeconds(60).verifyWith(MOJANG_PUBLIC_KEY).json(GSON_DESERIALIZER).build().parseSignedClaims(bedrockSession.getMcChain().getMojangJwt());
        final ECPublicKey mojangJwtPublicKey = publicKeyFromBase64(mojangJwt.getPayload().get("identityPublicKey", String.class));
        final Jws<Claims> identityJwt = Jwts.parser().clockSkewSeconds(60).verifyWith(mojangJwtPublicKey).build().parseSignedClaims(bedrockSession.getMcChain().getIdentityJwt());

        final String selfSignedJwt = Jwts.builder()
                .signWith(bedrockSession.getMcChain().getPrivateKey(), Jwts.SIG.ES384)
                .header().add("x5u", encodedPublicKey).and()
                .claim("certificateAuthority", true)
                .claim("identityPublicKey", mojangJwt.getHeader().get("x5u"))
                .expiration(Date.from(Instant.now().plus(2, ChronoUnit.DAYS)))
                .notBefore(Date.from(Instant.now().minus(1, ChronoUnit.MINUTES)))
                .compact();

        Map<String, Object> extraData = identityJwt.getPayload().get("extraData", Map.class);
        playerInfo = new PlayerInfo(
                (String) extraData.get("displayName"),
                (String) extraData.get("XUID"),
                (String) extraData.get("identity")
        );
        loginPacket.getChain().add(selfSignedJwt);
        loginPacket.getChain().add(bedrockSession.getMcChain().getMojangJwt());
        loginPacket.getChain().add(bedrockSession.getMcChain().getIdentityJwt());
        if (skinData == null) {
            skinData = new JSONObject();
        }
        skinData.put("ServerAddress", ProxyServer.getInstance().getDownstreamAddress() + ":" + ProxyServer.getInstance().getDownstreamPort());
        skinData.put("ThirdPartyName", playerInfo.username());
        skinData.put("SelfSignedId", playerInfo.uuid());
        skinData.put("SkinId", "Custom" + playerInfo.uuid());
        skinData.put("OverrideSkin", 1);
        skinData.put("PlayFabId", bedrockSession.getPlayFabToken().getPlayFabId());
        skinData.put("CompatibleWithClientSideChunkGen", false);
        skinData.put("DeviceId", bedrockSession.getMcChain().getXblXsts().getInitialXblSession().getXblDeviceToken().getDeviceId());
        Map<String, Object> skinDataMap = new HashMap<>();
        skinData.keySet().forEach((key) -> skinDataMap.put((String) key, skinData.get(key)));
        final String skinDataJwt = Jwts.builder()
                .signWith(bedrockSession.getMcChain().getPrivateKey(), Jwts.SIG.ES384)
                .header().add("x5u", encodedPublicKey).and()
                .claims(skinDataMap)
                .compact();
        loginPacket.setExtra(skinDataJwt);
        loginPacket.setProtocolVersion(ProxyServer.BEDROCK_CODEC.getProtocolVersion());
        return loginPacket;
    }
}
