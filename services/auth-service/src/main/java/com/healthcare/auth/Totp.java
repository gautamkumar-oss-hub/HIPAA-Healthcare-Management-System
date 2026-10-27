package com.healthcare.auth;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

@Component
public class Totp {
    private static final int PERIOD_SECONDS = 30;
    private static final int DIGITS = 6;
    private final SecureRandom random = new SecureRandom();

    public byte[] newSecret() {
        byte[] secret = new byte[20];
        random.nextBytes(secret);
        return secret;
    }

    public String provisioningUri(String email, byte[] secret, String issuer) {
        String encoded = base32(secret);
        return "otpauth://totp/" + issuer + ":" + email + "?secret=" + encoded
                + "&issuer=" + issuer + "&algorithm=SHA1&digits=6&period=30";
    }

    public boolean verify(byte[] secret, String suppliedCode) {
        if (suppliedCode == null || !suppliedCode.matches("\\d{6}")) {
            return false;
        }
        long currentStep = System.currentTimeMillis() / 1000 / PERIOD_SECONDS;
        for (long step = currentStep - 1; step <= currentStep + 1; step++) {
            if (codeForStep(secret, step).equals(suppliedCode)) {
                return true;
            }
        }
        return false;
    }

    private String codeForStep(byte[] secret, long step) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret, "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(step).array());
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            return "%06d".formatted(binary % (int) Math.pow(10, DIGITS));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not verify MFA code", exception);
        }
    }

    String currentCode(byte[] secret) {
        return codeForStep(secret, System.currentTimeMillis() / 1000 / PERIOD_SECONDS);
    }

    private static String base32(byte[] input) {
        final char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
        StringBuilder output = new StringBuilder((input.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsInBuffer = 0;
        for (byte value : input) {
            buffer = (buffer << 8) | (value & 0xff);
            bitsInBuffer += 8;
            while (bitsInBuffer >= 5) {
                output.append(alphabet[(buffer >> (bitsInBuffer - 5)) & 0x1f]);
                bitsInBuffer -= 5;
            }
        }
        if (bitsInBuffer > 0) {
            output.append(alphabet[(buffer << (5 - bitsInBuffer)) & 0x1f]);
        }
        return output.toString();
    }
}
