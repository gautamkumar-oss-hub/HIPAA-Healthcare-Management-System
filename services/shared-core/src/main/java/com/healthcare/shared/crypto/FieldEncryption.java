package com.healthcare.shared.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class FieldEncryption {
    private static final int NONCE_LENGTH = 12;
    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public FieldEncryption(@Value("${healthcare.security.field-encryption-key}") String encodedKey) {
        byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
        if (keyBytes.length != 32) {
            throw new IllegalStateException("FIELD_ENCRYPTION_KEY must decode to exactly 32 bytes");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    public byte[] encrypt(String plaintext) {
        byte[] nonce = new byte[NONCE_LENGTH];
        random.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] output = Arrays.copyOf(nonce, nonce.length + encrypted.length);
            System.arraycopy(encrypted, 0, output, nonce.length, encrypted.length);
            return output;
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not encrypt sensitive field", exception);
        }
    }

    public String decrypt(byte[] ciphertext) {
        if (ciphertext == null || ciphertext.length <= NONCE_LENGTH) {
            throw new IllegalArgumentException("Invalid encrypted field");
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key,
                    new GCMParameterSpec(128, ciphertext, 0, NONCE_LENGTH));
            return new String(cipher.doFinal(ciphertext, NONCE_LENGTH, ciphertext.length - NONCE_LENGTH),
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not decrypt sensitive field", exception);
        }
    }
}
