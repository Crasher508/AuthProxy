package dev.crasher508.authproxy.bedrock.network.session;

import org.cloudburstmc.protocol.bedrock.BedrockSession;

public interface ProxySession {

    BedrockSession getSendSession();

    void setSendSession(BedrockSession session);
}