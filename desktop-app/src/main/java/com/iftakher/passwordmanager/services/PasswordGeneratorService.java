package com.iftakher.passwordmanager.services;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class PasswordGeneratorService {
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String NUMBERS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?";
    private static final SecureRandom random = new SecureRandom();

    public static String generatePassword(int length, boolean includeUppercase, 
                                        boolean includeNumbers, boolean includeSymbols) {
        if (length < 8) {
            throw new IllegalArgumentException("Password length must be at least 8 characters");
        }

        StringBuilder charPool = new StringBuilder(LOWERCASE);
        if (includeUppercase) charPool.append(UPPERCASE);
        if (includeNumbers) charPool.append(NUMBERS);
        if (includeSymbols) charPool.append(SYMBOLS);

        // Ensure at least one character from each selected set
        List<Character> password = new ArrayList<>();
        
        // Add one lowercase
        password.add(LOWERCASE.charAt(random.nextInt(LOWERCASE.length())));
        
        if (includeUppercase) {
            password.add(UPPERCASE.charAt(random.nextInt(UPPERCASE.length())));
        }
        if (includeNumbers) {
            password.add(NUMBERS.charAt(random.nextInt(NUMBERS.length())));
        }
        if (includeSymbols) {
            password.add(SYMBOLS.charAt(random.nextInt(SYMBOLS.length())));
        }

        // Fill remaining characters
        while (password.size() < length) {
            password.add(charPool.charAt(random.nextInt(charPool.length())));
        }

        // Shuffle the password
        for (int i = password.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Character temp = password.get(i);
            password.set(i, password.get(j));
            password.set(j, temp);
        }

        // Convert to string
        StringBuilder result = new StringBuilder();
        for (Character c : password) {
            result.append(c);
        }

        return result.toString();
    }

    public static String assessPasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            return "Very Weak";
        }

        int score = 0;
        
        // Length score
        if (password.length() >= 12) score += 2;
        else if (password.length() >= 8) score += 1;

        // Character variety score
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSymbol = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\",./<>?].*");

        if (hasLower && hasUpper) score += 1;
        if (hasDigit) score += 1;
        if (hasSymbol) score += 1;
        if (hasLower && hasUpper && hasDigit && hasSymbol) score += 1;

        if (score >= 6) return "Very Strong";
        if (score >= 4) return "Strong";
        if (score >= 3) return "Good";
        if (score >= 2) return "Fair";
        return "Weak";
    }
}