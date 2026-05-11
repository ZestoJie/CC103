package com.cc103sys.cc103.Utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class CredentialsManager {
    private static final Logger LOGGER = Logger.getLogger(CredentialsManager.class.getName());
    private static final String CREDENTIALS_FILE = "user_prefs/credentials.dat";
    private static final String KEY_FILE = "user_prefs/key.key";
    private static final String ALGORITHM = "AES";
    private static SecretKey secretKey;

    static {
        initializeEncryption();
    }

    private static void initializeEncryption() {
        try {
            new File("user_prefs").mkdirs();
            File keyFile = new File(KEY_FILE);

            if (keyFile.exists()) {
                loadKey();
            } else {
                generateAndSaveKey();
            }
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to initialize encryption: " + e.getMessage());
        }
    }

    private static void generateAndSaveKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(256);
            secretKey = keyGen.generateKey();

            byte[] encodedKey = secretKey.getEncoded();
            Files.write(Paths.get(KEY_FILE), Base64.getEncoder().encode(encodedKey));
            LOGGER.info("Encryption key generated and saved");
        } catch (java.security.NoSuchAlgorithmException | java.io.IOException e) {
            LOGGER.severe(() -> "Failed to generate key: " + e.getMessage());
        }
    }

    private static void loadKey() {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(Files.readAllBytes(Paths.get(KEY_FILE)));
            secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);
            LOGGER.info("Encryption key loaded");
        } catch (java.io.IOException | IllegalArgumentException e) {
            LOGGER.severe(() -> "Failed to load key: " + e.getMessage());
            generateAndSaveKey();
        }
    }

    public static void saveCredentials(String username, String password) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            return;
        }

        try {
            String combined = username + "|" + password;
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(combined.getBytes());
            Files.write(Paths.get(CREDENTIALS_FILE), Base64.getEncoder().encode(encrypted));
            LOGGER.info("Credentials saved successfully");
        } catch (java.security.NoSuchAlgorithmException | javax.crypto.NoSuchPaddingException | java.security.InvalidKeyException | javax.crypto.IllegalBlockSizeException | javax.crypto.BadPaddingException | java.io.IOException e) {
            LOGGER.severe(() -> "Failed to save credentials: " + e.getMessage());
        }
    }

    public static String[] loadCredentials() {
        try {
            File file = new File(CREDENTIALS_FILE);
            if (!file.exists()) {
                return null;
            }

            byte[] encrypted = Base64.getDecoder().decode(Files.readAllBytes(Paths.get(CREDENTIALS_FILE)));
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decrypted = cipher.doFinal(encrypted);
            String combined = new String(decrypted);
            
            String[] parts = combined.split("\\|");
            if (parts.length == 2) {
                LOGGER.info("Credentials loaded successfully");
                return parts;
            }
            return null;
        } catch (java.security.NoSuchAlgorithmException | javax.crypto.NoSuchPaddingException | java.security.InvalidKeyException | javax.crypto.IllegalBlockSizeException | javax.crypto.BadPaddingException | java.io.IOException | IllegalArgumentException e) {
            LOGGER.severe(() -> "Failed to load credentials: " + e.getMessage());
            return null;
        }
    }

    public static void clearCredentials() {
        try {
            File file = new File(CREDENTIALS_FILE);
            if (file.exists() && file.delete()) {
                LOGGER.info("Credentials cleared");
            }
        } catch (Exception e) {
            LOGGER.severe(() -> "Failed to clear credentials: " + e.getMessage());
        }
    }
}
