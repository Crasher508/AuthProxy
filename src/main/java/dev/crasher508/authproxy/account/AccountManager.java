/*
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package dev.crasher508.authproxy.account;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.crasher508.authproxy.utils.Console;
import dev.crasher508.authproxy.utils.FileManager;
import dev.crasher508.authproxy.utils.TextFormat;
import lombok.Getter;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.security.MessageDigest;
import java.util.Base64;


public class AccountManager {

    private static AccountManager instance;

    public static AccountManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AccountManager need to be initialized first!");
        }
        return instance;
    }

    private final static String ENCRYPTION_ALGORITHM = "AES";
    private final static String HASH_ALGORITHM = "SHA-256";

    private final String secretKey;
    @Getter
    private final String fileName;
    private final JsonObject accounts;

    public AccountManager(String secretKey, String fileName) {
        this.secretKey = secretKey;
        this.fileName = fileName;
        instance = this;
        JsonObject accounts = new JsonObject();

        File accountFile = new File(fileName);
        if (!accountFile.exists()) {
            try {
                String encrypted = AccountManager.getInstance().encrypt(accounts.toString());
                FileManager.writeToFile(fileName, encrypted);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
        try {
            String json = this.decrypt(FileManager.getFileContents(fileName));
            accounts = JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception exception) {
            Console.writeLn(TextFormat.RED + exception.getMessage());
        }
        this.accounts = accounts;
    }

    public JsonObject getAccountByName(String name) {
        if (accounts == null) {
            return null;
        }
        return accounts.getAsJsonObject(name);
    }

    public String encrypt(String plaintext) throws Exception {
        SecretKey secretKey = this.generateSecretKey();
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public String decrypt(String ciphertext) throws Exception {
        SecretKey secretKey = this.generateSecretKey();
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
        return new String(decryptedBytes);
    }

    private SecretKey generateSecretKey() throws Exception {
        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        byte[] keyBytes = digest.digest(this.secretKey.getBytes());
        return new SecretKeySpec(keyBytes, ENCRYPTION_ALGORITHM);
    }

    public boolean saveAccount(String name, JsonObject account) {
        this.accounts.add(name, account);
        try {
            String encrypted = AccountManager.getInstance().encrypt(this.accounts.toString());
            FileManager.writeToFile(fileName, encrypted);
        } catch (Exception exception) {
            Console.writeLn(TextFormat.RED + exception.getMessage());
            return false;
        }
        return true;
    }
}
