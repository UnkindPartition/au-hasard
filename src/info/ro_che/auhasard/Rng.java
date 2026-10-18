package info.ro_che.auhasard;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Unbiased random draws built on a {@link RandomSource}. */
public final class Rng {
    private Rng() {}

    /**
     * Uniform in [0, n), where n is unsigned and 0 stands for 2^64.
     *
     * <p>Rejects the lowest 2^64 mod n words, so the remaining ones split evenly
     * among the n residues.
     */
    static long below(RandomSource rnd, long n) {
        if (n == 0) {
            return rnd.nextLong();
        }
        long threshold = Long.remainderUnsigned(-n, n);
        long x;
        do {
            x = rnd.nextLong();
        } while (Long.compareUnsigned(x, threshold) < 0);
        return Long.remainderUnsigned(x, n);
    }

    /** Uniform in [min, max]. */
    public static long uniform(RandomSource rnd, long min, long max) {
        checkRange(min, max);
        return min + below(rnd, max - min + 1);
    }

    /** {@code count} independent uniform draws from [min, max]. */
    public static long[] numbers(RandomSource rnd, long min, long max, int count) {
        checkRange(min, max);
        checkCount(count);
        long[] result = new long[count];
        for (int i = 0; i < count; i++) {
            result[i] = min + below(rnd, max - min + 1);
        }
        return result;
    }

    /**
     * {@code count} distinct numbers from [min, max], uniformly distributed over all
     * ordered selections.
     */
    public static long[] distinctNumbers(RandomSource rnd, long min, long max, int count) {
        checkRange(min, max);
        checkCount(count);
        long n = max - min + 1;
        if (n != 0 && Long.compareUnsigned(count, n) > 0) {
            throw new IllegalArgumentException("count exceeds the size of the range");
        }
        // Floyd's algorithm yields a uniform subset; the shuffle makes the order uniform.
        Set<Long> seen = new HashSet<>();
        long[] result = new long[count];
        long j = n - count;
        for (int i = 0; i < count; i++, j++) {
            long t = below(rnd, j + 1);
            if (!seen.add(t)) {
                seen.add(j);
                t = j;
            }
            result[i] = t;
        }
        for (int i = count - 1; i > 0; i--) {
            int k = (int) below(rnd, i + 1);
            long tmp = result[i];
            result[i] = result[k];
            result[k] = tmp;
        }
        for (int i = 0; i < count; i++) {
            result[i] += min;
        }
        return result;
    }

    /**
     * {@code count} distinct elements of {@code items}, uniformly distributed over all
     * ordered selections. The input is not modified.
     */
    public static <T> List<T> pick(RandomSource rnd, List<T> items, int count) {
        checkCount(count);
        if (count > items.size()) {
            throw new IllegalArgumentException("count exceeds the number of items");
        }
        // Partial Fisher-Yates shuffle.
        List<T> a = new ArrayList<>(items);
        for (int i = 0; i < count; i++) {
            int k = i + (int) below(rnd, a.size() - i);
            T tmp = a.get(i);
            a.set(i, a.get(k));
            a.set(k, tmp);
        }
        return new ArrayList<>(a.subList(0, count));
    }

    /** A uniformly random permutation of {@code items}. The input is not modified. */
    public static <T> List<T> shuffle(RandomSource rnd, List<T> items) {
        return pick(rnd, items, items.size());
    }

    private static void checkRange(long min, long max) {
        if (min > max) {
            throw new IllegalArgumentException("min > max");
        }
    }

    private static void checkCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("negative count");
        }
    }
}
