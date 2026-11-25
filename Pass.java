import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Pass {

    // Convert password into SHA-256 hash
    public static String hashPassword(String password) {

        try {
            MessageDigest md =
                MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = md.digest(
                password.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder hexString = new StringBuilder();

            for (byte b : hashBytes) {

                String hex = Integer.toHexString(0xff & b);

                if (hex.length() == 1) {
                    hexString.append('0');
                }

                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException(
                "SHA-256 algorithm is unavailable", e
            );
        }
    }

    // Verify entered password against stored hash
    public static boolean verifyPassword(
            String enteredPassword,
            String storedHash) {

        if (enteredPassword == null || storedHash == null) {
            return false;
        }

        String enteredHash = hashPassword(enteredPassword);

        return MessageDigest.isEqual(
            enteredHash.getBytes(StandardCharsets.UTF_8),
            storedHash.getBytes(StandardCharsets.UTF_8)
        );
    }
}