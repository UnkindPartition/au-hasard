package info.ro_che.auhasard;

import java.util.List;

/**
 * The app's draws, from the random number generator to results. Each draw first mixes the time
 * of the tap that asked for it into the generator's key. The app and its statistical tests both
 * go through this class.
 */
final class Draws {
    /** Shuffling numbers shows at most this many. */
    static final int MAX_SHUFFLED_NUMBERS = 100;

    private final ChaCha20Source rnd;

    Draws(ChaCha20Source rnd) {
        this.rnd = rnd;
    }

    static Draws fromKernel() {
        return new Draws(ChaCha20Source.fromKernel());
    }

    /** A number from [min, max]. */
    long number(long min, long max, long tap) {
        rnd.mix(tap);
        return Rng.uniform(rnd, min, max);
    }

    /**
     * The numbers of [min, max] in random order, or if there are more than
     * {@link #MAX_SHUFFLED_NUMBERS}, that many different ones from it.
     */
    long[] shuffledNumbers(long min, long max, long tap) {
        rnd.mix(tap);
        long size = max - min + 1;
        int count =
                size == 0 || Long.compareUnsigned(size, MAX_SHUFFLED_NUMBERS) > 0
                        ? MAX_SHUFFLED_NUMBERS
                        : (int) size;
        return Rng.distinctNumbers(rnd, min, max, count);
    }

    <T> T item(List<T> items, long tap) {
        rnd.mix(tap);
        return Rng.pick(rnd, items, 1).get(0);
    }

    <T> List<T> shuffledItems(List<T> items, long tap) {
        rnd.mix(tap);
        return Rng.shuffle(rnd, items);
    }

    // Values for the rolling animation, shown before a result lands. They don't affect later
    // draws, since mixing derives the new key from the key alone, not from the output so far.

    long reelNumber(long min, long max) {
        return Rng.uniform(rnd, min, max);
    }

    <T> T reelItem(List<T> items) {
        return Rng.pick(rnd, items, 1).get(0);
    }
}
