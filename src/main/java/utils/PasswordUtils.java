package utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilitaire pour le hachage et la vérification des mots de passe
 * Utilise l'algorithme SHA-256
 * @author COMLAN
 */
public class PasswordUtils {

    /**
     * Hache un mot de passe en utilisant SHA-256
     * @param password Le mot de passe en clair
     * @return Le mot de passe hashé en hexadécimal
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(password.getBytes());
            return bytesToHex(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erreur de hachage du mot de passe", e);
        }
    }

    /**
     * Vérifie si un mot de passe en clair correspond à un hash
     * @param password Le mot de passe en clair à vérifier
     * @param hash Le hash stocké en base de données
     * @return true si le mot de passe correspond, false sinon
     */
    public static boolean verifyPassword(String password, String hash) {
        String passwordHash = hashPassword(password);
        return passwordHash.equals(hash);
    }

    /**
     * Convertit un tableau de bytes en chaîne hexadécimale
     * @param bytes Le tableau de bytes
     * @return La représentation hexadécimale
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
