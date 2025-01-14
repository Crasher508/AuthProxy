package dev.crasher508.authproxy.bedrock.network.session;

import io.netty.util.ReferenceCountUtil;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.protocol.bedrock.BedrockPeer;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.BedrockSession;
import org.cloudburstmc.protocol.bedrock.netty.BedrockPacketWrapper;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.common.PacketSignal;

@Setter
@Getter
public class ProxyServerSession extends BedrockServerSession implements ProxySession {

    private BedrockSession sendSession;

    public ProxyServerSession(BedrockPeer peer, int subClientId) {
        super(peer, subClientId);
    }

    @Override
    protected void onPacket(BedrockPacketWrapper wrapper) {
        BedrockPacket packet = wrapper.getPacket();
        //System.out.println(packet.toString());

        if (this.packetHandler == null) {
            System.err.println("Received packet without a packet handler for " + this.getSocketAddress() + ":" + this.subClientId + ": " + packet);
        } else if (this.packetHandler.handlePacket(packet) == PacketSignal.UNHANDLED && this.sendSession != null) {
            this.sendSession.sendPacket(ReferenceCountUtil.retain(packet));
        }
    }
}