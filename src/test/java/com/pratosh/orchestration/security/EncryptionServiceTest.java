package com.pratosh.orchestration.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    /*
     * Creates an EncryptionService using a test-only AES key.
     *
     * This key is for automated testing only.
     * Never use this fixed key for real payment credentials.
     */
    private EncryptionService createEncryptionService() {

        // Create a 32-byte key for AES-256 testing.
        byte[] testKey = new byte[32];

        for (int i = 0; i < testKey.length; i++) {
            testKey[i] = (byte) (i + 1);
        }

        // EncryptionService expects the key in Base64 format.
        String encodedKey = Base64.getEncoder()
                .encodeToString(testKey);

        return new EncryptionService(encodedKey);
    }

    /*
     * Test 1:
     * Encrypt a value and verify that decrypting it
     * returns the original value.
     */
    @Test
    void encryptThenDecryptReturnsOriginalValue() {

        EncryptionService encryptionService =
                createEncryptionService();

        String originalSecret = "dummy-test-merchant-key";

        String encryptedSecret =
                encryptionService.encrypt(originalSecret);

        assertNotNull(encryptedSecret);

        // The encrypted value should not equal the plaintext.
        assertNotEquals(originalSecret, encryptedSecret);

        // Decryption must recover the original secret.
        assertEquals(
                originalSecret,
                encryptionService.decrypt(encryptedSecret)
        );
    }

    /*
     * Test 2:
     * Verify that encrypting the same plaintext twice
     * produces different ciphertexts because a fresh
     * random nonce is generated for each encryption.
     */
    @Test
    void samePlaintextProducesDifferentCiphertext() {

        EncryptionService encryptionService =
                createEncryptionService();

        String originalSecret = "dummy-test-secret";

        String firstEncryptedValue =
                encryptionService.encrypt(originalSecret);

        String secondEncryptedValue =
                encryptionService.encrypt(originalSecret);

        assertNotEquals(
                firstEncryptedValue,
                secondEncryptedValue
        );

        // Both ciphertexts must still decrypt correctly.
        assertEquals(
                originalSecret,
                encryptionService.decrypt(firstEncryptedValue)
        );

        assertEquals(
                originalSecret,
                encryptionService.decrypt(secondEncryptedValue)
        );
    }

    /*
     * Test 3:
     * Verify that modifying encrypted data causes
     * authentication to fail.
     */
    @Test
    void modifiedCiphertextIsRejected() {

        EncryptionService encryptionService =
                createEncryptionService();

        String encryptedSecret =
                encryptionService.encrypt("dummy-test-secret");

        byte[] encryptedBytes =
                Base64.getDecoder().decode(encryptedSecret);

        // Change the final byte to simulate corrupted data.
        encryptedBytes[encryptedBytes.length - 1] ^= 1;

        String modifiedCiphertext =
                Base64.getEncoder().encodeToString(encryptedBytes);

        assertThrows(
                IllegalStateException.class,
                () -> encryptionService.decrypt(modifiedCiphertext)
        );
    }

    /*
     * Test 4:
     * Verify that null inputs return null,
     * as implemented in EncryptionService.
     */
    @Test
    void nullValuesAreHandledCorrectly() {

        EncryptionService encryptionService =
                createEncryptionService();

        assertNull(encryptionService.encrypt(null));

        assertNull(encryptionService.decrypt(null));
    }
}
