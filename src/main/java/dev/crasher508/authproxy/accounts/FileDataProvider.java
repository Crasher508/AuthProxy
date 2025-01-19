package dev.crasher508.authproxy.accounts;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.crasher508.authproxy.utils.Console;
import dev.crasher508.authproxy.utils.FileManager;
import dev.crasher508.authproxy.utils.TextFormat;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FileDataProvider extends AbstractDataProvider {

    private final JsonObject cache;

    public FileDataProvider(String storageAddress, String storagePassword) {
        super(storageAddress, storagePassword);
        JsonObject accounts = new JsonObject();
        File accountFile = new File(storageAddress); //init
        if (!accountFile.exists()) {
            try {
                String encrypted = this.encrypt(accounts.toString());
                FileManager.writeToFile(storageAddress, encrypted);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
        try {
            String json = this.decrypt(FileManager.getFileContents(storageAddress));
            accounts = JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception exception) {
            Console.writeLn(TextFormat.RED + exception.getMessage());
        }
        this.cache = accounts;
    }

    @Override
    public Map<String, JsonObject> getAllAccounts() {
        Map<String, JsonObject> accounts = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : this.cache.entrySet()) {
            if (entry.getValue().isJsonObject()) {
                accounts.put(entry.getKey(), entry.getValue().getAsJsonObject());
            }
        }
        return accounts;
    }

    @Override
    public JsonObject getAccountByName(String name) {
        if (this.cache == null) {
            return null;
        }
        return this.cache.getAsJsonObject(name);
    }

    @Override
    public boolean saveAccount(String name, JsonObject account) {
        this.cache.add(name, account);
        try {
            String encrypted = this.encrypt(this.cache.toString());
            FileManager.writeToFile(this.storageAddress, encrypted);
        } catch (Exception exception) {
            Console.writeLn(TextFormat.RED + exception.getMessage());
            return false;
        }
        return true;
    }
}
