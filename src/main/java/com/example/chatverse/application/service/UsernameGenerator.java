package com.example.chatverse.application.service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class UsernameGenerator {

    private static final String ALGORITHM = "AES";
    private static final String SECRET_KEY = "mysecretkey12345"; // Секретный ключ для шифрования/дешифрования

    // Генерация уникального username на основе телефонного номера и секретного кода
    public static String generateUsernameFromPhone(String phone) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] encryptedPhone = cipher.doFinal(phone.getBytes());
        return Base64.getEncoder().encodeToString(encryptedPhone);
    }

    // Восстановление телефона из закодированного username
    public static String decodeUsernameToPhone(String encodedUsername) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        byte[] decodedBytes = Base64.getDecoder().decode(encodedUsername);
        byte[] decryptedPhone = cipher.doFinal(decodedBytes);

        return new String(decryptedPhone);
    }

    public static void main(String[] args) {
        try {
            String phone = "+1234567890";
            String username = generateUsernameFromPhone(phone);
            System.out.println("Generated Username: " + username);

            String decodedPhone = decodeUsernameToPhone(username);
            System.out.println("Decoded Phone: " + decodedPhone);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

