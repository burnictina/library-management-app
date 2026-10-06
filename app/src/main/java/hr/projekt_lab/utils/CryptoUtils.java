package hr.projekt_lab.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Pomoćna klasa za AES simetrično šifriranje i dešifriranje.
 *
 * Algoritam: AES/CBC/PKCS5Padding s nasumičnim 16-bajtnim IV-om.
 * Ključ: 256-bitni AES ključ izveden SHA-256 hashom iz definirane lozinke.
 * Format šifrirane datoteke: [IV (16 B)] | [šifrirani podaci]
 *
 * Lozinka se učitava iz okolišne varijable PROJEKT_LAB_AES_KEY;
 * ako nije postavljena, koristi se rezervna vrijednost iz koda.
 */
public final class CryptoUtils {

    private static final String AES_PASSPHRASE =
            System.getenv().getOrDefault("PROJEKT_LAB_AES_KEY", "library-aes-key-v1-projekt");

    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM     = "AES";
    private static final int    IV_LENGTH_BYTES   = 16;

    private CryptoUtils() {}

    /**
     * Šifrira niz bajtova AES/CBC algoritmom.
     * Nasumičan IV se dodaje na početak vraćenog niza: [IV (16 B)][šifrirati].
     *
     * @param plaintext podaci za šifriranje
     * @return [IV (16 B)] + [šifrirani podaci]
     */
    public static byte[] encrypt(byte[] plaintext) {
        try {
            byte[] iv = generateIv();
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(), new IvParameterSpec(iv));
            byte[] ciphertext = cipher.doFinal(plaintext);

            byte[] result = new byte[IV_LENGTH_BYTES + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, IV_LENGTH_BYTES);
            System.arraycopy(ciphertext, 0, result, IV_LENGTH_BYTES, ciphertext.length);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("AES šifriranje nije uspjelo.", e);
        }
    }

    /**
     * Dešifrira niz bajtova prethodno šifriranih metodom {@link #encrypt(byte[])}.
     * Očekuje format: [IV (16 B)] | [šifrirani podaci].
     *
     * @param data šifrirani podaci s IV prefiksom
     * @return dešifrirani podaci
     */
    public static byte[] decrypt(byte[] data) {
        if (data == null || data.length <= IV_LENGTH_BYTES) {
            throw new IllegalArgumentException("Podaci su prekratki za dešifriranje.");
        }
        try {
            byte[] iv         = new byte[IV_LENGTH_BYTES];
            byte[] ciphertext = new byte[data.length - IV_LENGTH_BYTES];
            System.arraycopy(data, 0, iv, 0, IV_LENGTH_BYTES);
            System.arraycopy(data, IV_LENGTH_BYTES, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(), new IvParameterSpec(iv));
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new RuntimeException("AES dešifriranje nije uspjelo.", e);
        }
    }

    private static SecretKeySpec deriveKey() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(AES_PASSPHRASE.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, KEY_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algoritam nije dostupan.", e);
        }
    }

    private static byte[] generateIv() {
        byte[] iv = new byte[IV_LENGTH_BYTES];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}
