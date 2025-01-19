package dev.crasher508.authproxy.accounts;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Base64;

public abstract class AbstractDataProvider implements DataProvider {

    private final static String ENCRYPTION_ALGORITHM = "AES";
    private final static String HASH_ALGORITHM = "SHA-256";

    protected final String storageAddress;
    protected final String storagePassword;

    public AbstractDataProvider(String storageAddress, String storagePassword) {
        this.storageAddress = storageAddress;
        this.storagePassword = storagePassword;
    }

    protected String encrypt(String plaintext) throws Exception {
        SecretKey secretKey = this.generateSecretKey();
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    protected String decrypt(String ciphertext) throws Exception {
        SecretKey secretKey = this.generateSecretKey();
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
        return new String(decryptedBytes);
    }

    private SecretKey generateSecretKey() throws Exception {
        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        byte[] keyBytes = digest.digest(this.storagePassword.getBytes());
        return new SecretKeySpec(keyBytes, ENCRYPTION_ALGORITHM);
    }
}
