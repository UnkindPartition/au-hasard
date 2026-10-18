package info.ro_che.auhasard;

import java.security.SecureRandom;

/** The operating system's cryptographically secure generator. */
public final class SecureRandomSource implements RandomSource {
    private final SecureRandom random = new SecureRandom();

    @Override
    public long nextLong() {
        return random.nextLong();
    }
}
