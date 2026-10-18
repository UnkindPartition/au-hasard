package info.ro_che.auhasard;

/** A source of uniformly distributed, independent 64-bit words. */
public interface RandomSource {
    long nextLong();
}
