package com.pratosh.orchestration.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EncryptionService {

    private static final int NONCE_LENGTH_BYTES = 12;
    private static final int AUTH_TAG_LENGTH_BITS = 128;
    private static final int AES_256_KEY_LENGTH_BYTES = 32;

    private static final String AES_GCM_TRANSFORMATION =
            "AES/GCM/NoPadding";

    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKey secretKey;

    public EncryptionService(
            @Value("${credential.encryption-key}")
            String encodedKey) {

        if (encodedKey == null || encodedKey.isBlank()) {
            throw new IllegalStateException(
                    "CREDENTIAL_ENCRYPTION_KEY is missing."
            );
        }

        final byte[] keyBytes;

        try {
            keyBytes = Base64.getDecoder().decode(encodedKey.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "CREDENTIAL_ENCRYPTION_KEY must be valid Base64.",
                    exception
            );
        }

        if (keyBytes.length != AES_256_KEY_LENGTH_BYTES) {
            throw new IllegalStateException(
                    "CREDENTIAL_ENCRYPTION_KEY must decode to exactly 32 bytes."
            );
        }

        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Encrypts a credential using AES-256-GCM.
     *
     * Stored format:
     * Base64(nonce + ciphertext + authentication tag)
     */
    public String encrypt(String plaintext) {

        if (plaintext == null) {
            return null;
        }

        if (plaintext.isEmpty()) {
            throw new IllegalArgumentException(
                    "Credential cannot be empty."
            );
        }

        try {
            byte[] nonce = new byte[NONCE_LENGTH_BYTES];
            secureRandom.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance(
                    AES_GCM_TRANSFORMATION
            );

            GCMParameterSpec parameterSpec =
                    new GCMParameterSpec(
                            AUTH_TAG_LENGTH_BITS,
                            nonce
                    );

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKey,
                    parameterSpec
            );

            byte[] ciphertext = cipher.doFinal(
                    plaintext.getBytes(StandardCharsets.UTF_8)
            );

            byte[] encryptedData =
                    new byte[nonce.length + ciphertext.length];

            System.arraycopy(
                    nonce,
                    0,
                    encryptedData,
                    0,
                    nonce.length
            );

            System.arraycopy(
                    ciphertext,
                    0,
                    encryptedData,
                    nonce.length,
                    ciphertext.length
            );

            return Base64.getEncoder()
                    .encodeToString(encryptedData);

        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Unable to encrypt the credential.",
                    exception
            );
        }
    }

    /**
     * Decrypts a credential previously produced by encrypt().
     *
     * The original encryption key must be available.
     */
    public String decrypt(String encryptedValue) {

        if (encryptedValue == null) {
            return null;
        }

        if (encryptedValue.isBlank()) {
            throw new IllegalArgumentException(
                    "Encrypted credential cannot be empty."
            );
        }

        try {
            byte[] encryptedData = Base64.getDecoder()
                    .decode(encryptedValue);

            int minimumLength =
                    NONCE_LENGTH_BYTES
                            + (AUTH_TAG_LENGTH_BITS / 8);

            if (encryptedData.length < minimumLength) {
                throw new IllegalArgumentException(
                        "Encrypted credential has an invalid format."
                );
            }

            byte[] nonce = new byte[NONCE_LENGTH_BYTES];

            System.arraycopy(
                    encryptedData,
                    0,
                    nonce,
                    0,
                    NONCE_LENGTH_BYTES
            );

            int ciphertextLength =
                    encryptedData.length - NONCE_LENGTH_BYTES;

            byte[] ciphertext = new byte[ciphertextLength];

            System.arraycopy(
                    encryptedData,
                    NONCE_LENGTH_BYTES,
                    ciphertext,
                    0,
                    ciphertextLength
            );

            Cipher cipher = Cipher.getInstance(
                    AES_GCM_TRANSFORMATION
            );

            GCMParameterSpec parameterSpec =
                    new GCMParameterSpec(
                            AUTH_TAG_LENGTH_BITS,
                            nonce
                    );

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    parameterSpec
            );

            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(
                    plaintext,
                    StandardCharsets.UTF_8
            );

        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Unable to decrypt credential. Verify the encryption key and stored data.",
                    exception
            );
        }
    }
}
