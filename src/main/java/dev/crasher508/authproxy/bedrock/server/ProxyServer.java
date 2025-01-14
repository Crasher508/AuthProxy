/*
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package dev.crasher508.authproxy.bedrock.server;

import dev.crasher508.authproxy.bedrock.network.handler.upstream.UpstreamPacketHandler;
import dev.crasher508.authproxy.bedrock.network.session.ProxyServerSession;
import dev.crasher508.authproxy.bedrock.player.ProxiedPlayer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.netty.channel.raknet.RakChannelFactory;
import org.cloudburstmc.netty.channel.raknet.config.RakChannelOption;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.BedrockPong;
import org.cloudburstmc.protocol.bedrock.codec.BedrockCodec;
import org.cloudburstmc.protocol.bedrock.codec.v766.Bedrock_v766;
import org.cloudburstmc.protocol.bedrock.netty.initializer.BedrockChannelInitializer;

import java.net.InetSocketAddress;

public class ProxyServer {

    @Getter
    private static ProxyServer instance;

    @Getter
    @Setter
    private boolean isRunning = false;

    public static final BedrockCodec BEDROCK_CODEC = Bedrock_v766.CODEC;

    private final InetSocketAddress bindAddress;

    @Getter
    private final String downstreamAddress;
    @Getter
    private final int downstreamPort;

    public ProxyServer(InetSocketAddress address, String downstreamAddress, int downstreamPort) {
        this.bindAddress = address;
        this.downstreamAddress = downstreamAddress;
        this.downstreamPort = downstreamPort;
        instance = this;
    }

    private static final BedrockPong ADVERTISEMENT = new BedrockPong()
            .edition("MCPE")
            .gameType("Survival")
            .version(ProxyServer.BEDROCK_CODEC.getMinecraftVersion())
            .protocolVersion(ProxyServer.BEDROCK_CODEC.getProtocolVersion())
            .playerCount(0)
            .maximumPlayerCount(20)
            .motd("BedrockProxy")
            .subMotd("Proxy Server")
            .nintendoLimited(false);

    public void start() {
        if (isRunning) {
            return;
        }

        ADVERTISEMENT.ipv4Port(bindAddress.getPort())
                .ipv6Port(bindAddress.getPort());
        new ServerBootstrap()
                .group(new NioEventLoopGroup())
                .channelFactory(RakChannelFactory.server(NioDatagramChannel.class))
                .option(RakChannelOption.RAK_ADVERTISEMENT, ADVERTISEMENT.toByteBuf())
                .childHandler(new BedrockChannelInitializer<ProxyServerSession>() {

                    @Override
                    protected ProxyServerSession createSession0(BedrockPeer peer, int subClientId) {
                        return new ProxyServerSession(peer, subClientId);
                    }

                    @Override
                    protected void initSession(ProxyServerSession session) {
                        ProxiedPlayer proxiedPlayer = new ProxiedPlayer(session);
                        session.setPacketHandler(new UpstreamPacketHandler(proxiedPlayer));
                    }
                })
                .bind(bindAddress)
                .awaitUninterruptibly()
                .channel();

        isRunning = true;
    }
}
