package com.iftakher.passwordmanager.services;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;


public class EncryptionService {
    public static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "AES";
    private static final String PBKDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    
    private static final int ITERATIONS = 100000;
    private static final int KEY_LENGTH = 256;
    private static final int IV_LENGTH = 16;
    private static final int SALT_LENGTH = 16;

    public static class EncryptedData {
        private final byte[] encryptedData;
        private final byte[] iv;

        public EncryptedData(byte[] encryptedData, byte[] iv) {
            this.encryptedData = encryptedData;
            this.iv = iv;
        }

        public byte[] getEncryptedData() { return encryptedData; }
        public byte[] getIv() { return iv; }
    }

    public static SecretKey generateKeyFromPassword(String password, byte[] salt) 
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF_ALGORITHM);
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), KEY_ALGORITHM);
    }

    public static EncryptedData encryptData(String data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        byte[] encrypted = cipher.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        
        return new EncryptedData(encrypted, iv);
    }

    public static String decryptData(EncryptedData encryptedData, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(encryptedData.getIv());
        
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        byte[] decrypted = cipher.doFinal(encryptedData.getEncryptedData());
        
        return new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
    }

    public static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    public static String hashPassword(String password) throws NoSuchAlgorithmException {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    public static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF_ALGORITHM);
        KeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKey tmp = factory.generateSecret(spec);
        return tmp.getEncoded();
    }
}