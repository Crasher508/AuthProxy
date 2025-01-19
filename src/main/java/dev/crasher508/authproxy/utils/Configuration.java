package dev.crasher508.authproxy.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jose4j.json.internal.json_simple.JSONObject;

@Getter
@AllArgsConstructor
public class Configuration {

    private final String storageProvider;
    private final String storageSecretKey;
    private final String storageAddress;

    private final String proxyAddress;
    private final int proxyPort;
    private final String proxyMotd;
    private final String proxySubMotd;

    private final String targetAddress;
    private final int targetPort;

    public static Configuration load(String configFileName) {
        JSONObject config = Json.parseJSONObject(FileManager.getFileContents(configFileName));

        JSONObject storageConfig = Json.parseJSONObjectFromObject(config, "storage");
        String provider = Json.parseStringFromJSONObject(storageConfig, "provider");
        String secretKey = Json.parseStringFromJSONObject(storageConfig, "secret_key");
        String fileName = Json.parseStringFromJSONObject(storageConfig, "file_name");

        JSONObject proxyConfig = Json.parseJSONObjectFromObject(config, "proxy");
        String hostname = Json.parseStringFromJSONObject(proxyConfig, "hostname");
        int port = Math.round(Json.parseLongFromJSONObject(proxyConfig, "port"));
        String motd = Json.parseStringFromJSONObject(proxyConfig, "motd");
        String sub_motd = Json.parseStringFromJSONObject(proxyConfig, "sub_motd");

        JSONObject serverConfig = Json.parseJSONObjectFromObject(config, "server");
        String downstreamAddress = Json.parseStringFromJSONObject(serverConfig, "address");
        int downstreamPort = Math.round(Json.parseLongFromJSONObject(serverConfig, "port"));

        return new Configuration(provider, secretKey, fileName, hostname, port, motd, sub_motd, downstreamAddress, downstreamPort);
    }
}
