package org.telegram.messenger;

import android.util.Base64;

import org.json.JSONObject;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * LuminaCrypto — the single passphrase-encryption implementation shared by every
 * LuminaGram "encrypted local file" feature (settings backup, chat export, ...).
 *
 * Scheme (unchanged from the original LuminaBackupActivity implementation, so old
 * .lgbak files keep opening): AES/CBC/PKCS5Padding under a 256-bit key derived from
 * the passphrase with PBKDF2WithHmacSHA256, a fresh random 16-byte salt and 16-byte
 * IV per file, both stored in the clear inside a small JSON envelope:
 *
 *   { magic, version, kdf, iter, cipher, salt(b64), iv(b64), data(b64) }
 *
 * It deliberately lives in one place: two features hand-rolling "almost the same"
 * construction is how key-derivation/IV bugs get introduced. Nothing here touches the
 * network — callers write the envelope to local storage only.
 */
public final class LuminaCrypto {

    public static final int FORMAT_VERSION = 1;
    public static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    public static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    public static final int PBKDF2_ITERATIONS = 120000;
    public static final int KEY_LENGTH_BITS = 256;
    public static final int SALT_LENGTH = 16;
    public static final int IV_LENGTH = 16;
    public static final int MIN_PASSPHRASE_LENGTH = 4;

    private LuminaCrypto() {}

    /** Encrypt {@code plaintext} under {@code passphrase} and return the JSON envelope. */
    public static JSONObject seal(String magic, byte[] plaintext, String passphrase) throws Exception {
        byte[] salt = randomBytes(SALT_LENGTH);
        byte[] iv = randomBytes(IV_LENGTH);
        SecretKey key = deriveKey(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] ciphertext = cipher.doFinal(plaintext);

        JSONObject env = new JSONObject();
        env.put("magic", magic);
        env.put("version", FORMAT_VERSION);
        env.put("kdf", KDF_ALGORITHM);
        env.put("iter", PBKDF2_ITERATIONS);
        env.put("cipher", TRANSFORMATION);
        env.put("salt", Base64.encodeToString(salt, Base64.NO_WRAP));
        env.put("iv", Base64.encodeToString(iv, Base64.NO_WRAP));
        env.put("data", Base64.encodeToString(ciphertext, Base64.NO_WRAP));
        return env;
    }

    /**
     * Decrypt an envelope produced by {@link #seal}. Exceptions are intentionally NOT
     * swallowed: a wrong passphrase surfaces as BadPaddingException/IllegalBlockSizeException,
     * which callers distinguish from "unreadable file" when picking the error message.
     */
    public static byte[] open(JSONObject envelope, String passphrase) throws Exception {
        byte[] salt = Base64.decode(envelope.optString("salt"), Base64.NO_WRAP);
        byte[] iv = Base64.decode(envelope.optString("iv"), Base64.NO_WRAP);
        byte[] ciphertext = Base64.decode(envelope.optString("data"), Base64.NO_WRAP);
        int iterations = envelope.optInt("iter", PBKDF2_ITERATIONS);

        SecretKey key = deriveKey(passphrase.toCharArray(), salt, iterations);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        return cipher.doFinal(ciphertext);
    }

    public static SecretKey deriveKey(char[] passphrase, byte[] salt, int iterations) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(passphrase, salt, iterations, KEY_LENGTH_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGORITHM);
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } finally {
            spec.clearPassword();
        }
    }

    public static byte[] randomBytes(int length) {
        byte[] out = new byte[length];
        new SecureRandom().nextBytes(out);
        return out;
    }
}
