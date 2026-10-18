package info.ro_che.auhasard;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.SplittableRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.ChaCha20ParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.Test;

public class ChaCha20Test {
    private static int[] words(String hex) {
        String[] parts = hex.trim().split("\\s+");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = (int) Long.parseLong(parts[i], 16);
        }
        return result;
    }

    private static byte[] bytes(String hex) {
        String s = hex.replaceAll("[\\s:]", "");
        byte[] result = new byte[s.length() / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        }
        return result;
    }

    /** The first {@code n} bytes of the source's output. */
    private static byte[] output(ChaCha20Source source, int n) {
        byte[] result = new byte[n];
        for (int i = 0; i < n; i += 8) {
            long v = source.nextLong();
            ChaCha20Source.putLittleEndian(result, i, (int) v);
            ChaCha20Source.putLittleEndian(result, i + 4, (int) (v >>> 32));
        }
        return result;
    }

    @Test
    @About("Checks the quarter round against the test vector of RFC 8439, section 2.1.1.")
    public void quarterRoundTestVector() {
        int[] x = words("11111111 01020304 9b8d6f43 01234567");
        ChaCha20Source.quarterRound(x, 0, 1, 2, 3);
        assertArrayEquals(words("ea2a92f4 cb1cf8ce 4581472e 5881c4bb"), x);
    }

    @Test
    @About(
            "Checks the block function against the test vector of RFC 8439, section 2.3.2: "
            + "key 00 01 ... 1f, block counter 1, nonce 00 00 00 09 00 00 00 4a 00 00 00 00.")
    public void blockTestVector() {
        int[] key = new int[8];
        byte[] keyBytes = bytes("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f");
        for (int i = 0; i < 8; i++) {
            key[i] = ChaCha20Source.getLittleEndian(keyBytes, 4 * i);
        }
        int[] out = new int[16];
        ChaCha20Source.block(key, 1, words("09000000 4a000000 00000000"), out);
        assertArrayEquals(
                words(
                        "e4e7f110 15593bd1 1fdd0f50 c47120a3"
                                + " c7f4d1c7 0368c033 9aaa2204 4e6cd4c3"
                                + " 466482d2 09aa9f07 05d7c214 a2028bd9"
                                + " d19c12b5 b94e16de e883d0cb 4e3c50a2"),
                out);
    }

    @Test
    @About(
            "Checks the generator's output for an all-zero key against the first keystream "
                    + "test vector of RFC 8439, appendix A.1.")
    public void zeroKeyTestVector() {
        assertArrayEquals(
                bytes(
                        "76b8e0ada0f13d90405d6ae55386bd28bdd219b8a08ded1aa836efcc8b770dc7"
                                + "da41597c5157488d7724e03fb8d84a376a43b8f41518a11cc387b669b2ee6586"),
                output(new ChaCha20Source(new byte[32]), 64));
    }

    @Test
    @About(
            "For 100 random keys, compares 1000 bytes of the generator's output, which spans "
                    + "several blocks, with the keystream of the JDK's independent ChaCha20 "
                    + "implementation (all-zero nonce, counter starting at 0).")
    public void matchesJdk() throws Exception {
        SplittableRandom rnd = new SplittableRandom(1);
        for (int k = 0; k < 100; k++) {
            byte[] key = new byte[32];
            rnd.nextBytes(key);
            Cipher jdk = Cipher.getInstance("ChaCha20");
            jdk.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(key, "ChaCha20"),
                    new ChaCha20ParameterSpec(new byte[12], 0));
            // Encrypting zeros yields the keystream itself.
            byte[] expected = jdk.doFinal(new byte[1000]);
            assertArrayEquals(expected, output(new ChaCha20Source(key), 1000));
        }
    }

    @Test
    @About(
            "Checks that mixing in a value replaces the key with the SHA-256 hash of the old key "
                    + "followed by the value (both little-endian), and restarts the keystream.")
    public void mixHashesKeyAndInput() throws Exception {
        SplittableRandom rnd = new SplittableRandom(2);
        byte[] key = new byte[32];
        rnd.nextBytes(key);
        long input = rnd.nextLong();

        ChaCha20Source source = new ChaCha20Source(key);
        source.nextLong();
        source.mix(input);

        byte[] data = new byte[40];
        System.arraycopy(key, 0, data, 0, 32);
        ChaCha20Source.putLittleEndian(data, 32, (int) input);
        ChaCha20Source.putLittleEndian(data, 36, (int) (input >>> 32));
        byte[] newKey = MessageDigest.getInstance("SHA-256").digest(data);
        assertArrayEquals(output(new ChaCha20Source(newKey), 64), output(source, 64));
    }

    @Test
    @About(
            "Checks that two generators keyed from /dev/urandom, and one generator before and "
                    + "after mixing in a value, produce different output.")
    public void outputsDiffer() {
        assertFalse(
                Arrays.equals(
                        output(ChaCha20Source.fromKernel(), 32),
                        output(ChaCha20Source.fromKernel(), 32)));

        ChaCha20Source mixed = new ChaCha20Source(new byte[32]);
        mixed.mix(0);
        assertFalse(Arrays.equals(output(new ChaCha20Source(new byte[32]), 32), output(mixed, 32)));
    }
}
