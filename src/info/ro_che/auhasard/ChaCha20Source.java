package info.ro_che.auhasard;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Random numbers from the ChaCha20 keystream (RFC 8439). The key is set from the kernel's random
 * number generator and refreshed with further unpredictable input by {@link #mix}.
 */
public final class ChaCha20Source implements RandomSource {
    private final int[] key = new int[8];
    /** Block counter and nonce, together a 128-bit counter, so that the keystream never repeats. */
    private int counter;
    private final int[] nonce = new int[3];
    private final int[] block = new int[16];
    /** The next unused word of the block; 16 when it's used up. */
    private int next;

    ChaCha20Source(byte[] key) {
        setKey(key);
    }

    /** A generator keyed with 32 bytes from /dev/urandom. */
    static ChaCha20Source fromKernel() {
        byte[] key = new byte[32];
        try (FileInputStream in = new FileInputStream("/dev/urandom")) {
            for (int n = 0; n < key.length; ) {
                int r = in.read(key, n, key.length - n);
                if (r < 0) {
                    throw new IOException("unexpected end of /dev/urandom");
                }
                n += r;
            }
        } catch (IOException e) {
            // Should the device file be unreadable, use the platform's generator instead.
            new SecureRandom().nextBytes(key);
        }
        return new ChaCha20Source(key);
    }

    @Override
    public long nextLong() {
        if (next == 16) {
            block(key, counter, nonce, block);
            if (++counter == 0 && ++nonce[0] == 0 && ++nonce[1] == 0) {
                nonce[2]++;
            }
            next = 0;
        }
        long low = block[next] & 0xffffffffL;
        long high = block[next + 1];
        next += 2;
        return low | high << 32;
    }

    /** Replaces the key with the SHA-256 hash of the key followed by {@code input}. */
    void mix(long input) {
        MessageDigest sha;
        try {
            sha = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        byte[] bytes = new byte[32 + 8];
        for (int i = 0; i < 8; i++) {
            putLittleEndian(bytes, 4 * i, key[i]);
        }
        putLittleEndian(bytes, 32, (int) input);
        putLittleEndian(bytes, 36, (int) (input >>> 32));
        setKey(sha.digest(bytes));
    }

    private void setKey(byte[] bytes) {
        for (int i = 0; i < 8; i++) {
            key[i] = getLittleEndian(bytes, 4 * i);
        }
        counter = 0;
        nonce[0] = nonce[1] = nonce[2] = 0;
        next = 16;
    }

    /** The ChaCha20 block function (RFC 8439, section 2.3), as 16 words. */
    static void block(int[] key, int counter, int[] nonce, int[] out) {
        int[] state = {
            0x61707865, 0x3320646e, 0x79622d32, 0x6b206574,
            key[0], key[1], key[2], key[3],
            key[4], key[5], key[6], key[7],
            counter, nonce[0], nonce[1], nonce[2],
        };
        int[] x = state.clone();
        for (int i = 0; i < 10; i++) {
            quarterRound(x, 0, 4, 8, 12);
            quarterRound(x, 1, 5, 9, 13);
            quarterRound(x, 2, 6, 10, 14);
            quarterRound(x, 3, 7, 11, 15);
            quarterRound(x, 0, 5, 10, 15);
            quarterRound(x, 1, 6, 11, 12);
            quarterRound(x, 2, 7, 8, 13);
            quarterRound(x, 3, 4, 9, 14);
        }
        for (int i = 0; i < 16; i++) {
            out[i] = x[i] + state[i];
        }
    }

    /** RFC 8439, section 2.1. */
    static void quarterRound(int[] x, int a, int b, int c, int d) {
        x[a] += x[b];
        x[d] = Integer.rotateLeft(x[d] ^ x[a], 16);
        x[c] += x[d];
        x[b] = Integer.rotateLeft(x[b] ^ x[c], 12);
        x[a] += x[b];
        x[d] = Integer.rotateLeft(x[d] ^ x[a], 8);
        x[c] += x[d];
        x[b] = Integer.rotateLeft(x[b] ^ x[c], 7);
    }

    static int getLittleEndian(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff)
                | (bytes[offset + 1] & 0xff) << 8
                | (bytes[offset + 2] & 0xff) << 16
                | (bytes[offset + 3] & 0xff) << 24;
    }

    static void putLittleEndian(byte[] bytes, int offset, int word) {
        bytes[offset] = (byte) word;
        bytes[offset + 1] = (byte) (word >>> 8);
        bytes[offset + 2] = (byte) (word >>> 16);
        bytes[offset + 3] = (byte) (word >>> 24);
    }
}
