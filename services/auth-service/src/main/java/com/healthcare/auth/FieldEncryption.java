package com.healthcare.auth;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

@Component
public class FieldEncryption {
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public FieldEncryption(SecretKey fieldEncryptionKey) {
        this.key = fieldEncryptionKey;
    }

    public byte[] encrypt(byte[] plaintext) {
        byte[] nonce = new byte[NONCE_LENGTH];
        random.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            byte[] encrypted = cipher.doFinal(plaintext);
            byte[] result = Arrays.copyOf(nonce, nonce.length + encrypted.length);
            System.arraycopy(encrypted, 0, result, nonce.length, encrypted.length);
            return result;
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not encrypt sensitive field", exception);
        }
    }

    public byte[] decrypt(byte[] ciphertext) {
        if (ciphertext == null || ciphertext.length <= NONCE_LENGTH) {
            throw new IllegalArgumentException("Invalid encrypted field");
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key,
                    new GCMParameterSpec(TAG_LENGTH_BITS, ciphertext, 0, NONCE_LENGTH));
            return cipher.doFinal(ciphertext, NONCE_LENGTH, ciphertext.length - NONCE_LENGTH);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not decrypt sensitive field", exception);
        }
    }
}
