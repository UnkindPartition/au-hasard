package info.ro_che.auhasard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * A statistical check of the app's draws ({@link Draws}), made the way the app makes them, with
 * successive numbers standing in for the times of taps. It computes a p-value.
 */
final class StatCheck {
    /** Significance level. */
    static final double ALPHA = 1e-3;

    static final class Outcome {
        final String summary;
        final double p;

        Outcome(String summary, double p) {
            this.summary = summary;
            this.p = p;
        }
    }

    final String name;
    private final Function<Draws, Outcome> run;

    private StatCheck(String name, Function<Draws, Outcome> run) {
        this.name = name;
        this.run = run;
    }

    Outcome run(Draws draws) {
        return run.apply(draws);
    }

    /** The app's generator, keyed with the SHA-256 hash of the seed (little-endian). */
    static ChaCha20Source generator(long seed) {
        byte[] bytes = new byte[8];
        ChaCha20Source.putLittleEndian(bytes, 0, (int) seed);
        ChaCha20Source.putLittleEndian(bytes, 4, (int) (seed >>> 32));
        try {
            return new ChaCha20Source(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static Draws seeded(long seed) {
        return new Draws(generator(seed));
    }

    // Numbers. Checks with few outcomes use many draws per outcome, since otherwise their
    // p-values take too few distinct values to be close to uniform.

    static final StatCheck NUMBER_2 = numbers(0, 1, 10000);
    static final StatCheck NUMBER_6 = numbers(1, 6, 2000);
    static final StatCheck NUMBER_100 = numbers(-50, 49, 200);
    static final StatCheck NUMBER_1000 = numbers(1, 1000, 50);

    // With plain x mod n, the first third of this range would come up twice as often as
    // each of the other two thirds.
    static final StatCheck MODULO_BIAS =
            new StatCheck(
                    "thirds of [-2^63, 2^62)",
                    draws -> {
                        long min = Long.MIN_VALUE;
                        long max = (1L << 62) - 1;
                        long[] counts = new long[3];
                        for (int tap = 0; tap < 3 * 5000; tap++) {
                            counts[(int) ((draws.number(min, max, tap) - min) >>> 62)]++;
                        }
                        return ChiSquare.uniform("thirds of [-2^63, 2^62)", counts);
                    });

    static final StatCheck FULL_RANGE =
            new StatCheck(
                    "top 4 bits of the full range",
                    draws -> {
                        long[] counts = new long[16];
                        for (int tap = 0; tap < 16 * 1000; tap++) {
                            long v = draws.number(Long.MIN_VALUE, Long.MAX_VALUE, tap);
                            counts[(int) (v >>> 60)]++;
                        }
                        return ChiSquare.uniform("top 4 bits of the full range", counts);
                    });

    static final StatCheck PAIRS =
            new StatCheck(
                    "pairs of draws from [0, 5]",
                    draws -> {
                        long[] counts = new long[36];
                        int tap = 0;
                        for (int i = 0; i < 36 * 500; i++) {
                            long a = draws.number(0, 5, tap++);
                            long b = draws.number(0, 5, tap++);
                            counts[(int) (a * 6 + b)]++;
                        }
                        return ChiSquare.uniform("pairs of draws from [0, 5]", counts);
                    });

    static final StatCheck TRIPLES =
            new StatCheck(
                    "triples of draws from [0, 3]",
                    draws -> {
                        long[] counts = new long[64];
                        int tap = 0;
                        for (int i = 0; i < 64 * 200; i++) {
                            long a = draws.number(0, 3, tap++);
                            long b = draws.number(0, 3, tap++);
                            long c = draws.number(0, 3, tap++);
                            counts[(int) (a * 16 + b * 4 + c)]++;
                        }
                        return ChiSquare.uniform("triples of draws from [0, 3]", counts);
                    });

    // Wald-Wolfowitz: the number of runs of increases and decreases in n continuous
    // i.i.d. values has mean (2n - 1) / 3 and variance (16n - 29) / 90.
    static final StatCheck RUNS =
            new StatCheck(
                    "runs up and down",
                    draws -> {
                        int n = 20000;
                        long[] v = new long[n];
                        for (int tap = 0; tap < n; tap++) {
                            v[tap] = draws.number(Long.MIN_VALUE, Long.MAX_VALUE, tap);
                        }
                        int runs = 1;
                        for (int i = 2; i < n; i++) {
                            if ((v[i] > v[i - 1]) != (v[i - 1] > v[i - 2])) {
                                runs++;
                            }
                        }
                        double mean = (2.0 * n - 1) / 3;
                        double z = (runs - mean) / Math.sqrt((16.0 * n - 29) / 90);
                        return new Outcome(
                                String.format(
                                        "runs up and down: %d runs in %d draws (expected %.0f),"
                                                + " z = %.2f",
                                        runs, n, mean, z),
                                ChiSquare.pValue(z * z, 1));
                    });

    // Shuffled numbers.

    static final StatCheck SHUFFLED_NUMBERS_4 =
            new StatCheck(
                    "orders of [1, 4]",
                    draws -> {
                        long[] counts = new long[256];
                        for (int tap = 0; tap < 24 * 500; tap++) {
                            long[] v = draws.shuffledNumbers(1, 4, tap);
                            counts[(int) ((v[0] - 1) * 64 + (v[1] - 1) * 16 + (v[2] - 1) * 4 + v[3] - 1)]++;
                        }
                        return ChiSquare.uniform("orders of [1, 4]", nonEmptyCells(counts, 24));
                    });

    // The largest range that is shuffled whole.
    static final StatCheck SHUFFLED_NUMBERS_100 =
            new StatCheck(
                    "positions of 1 in orders of [1, 100]",
                    draws -> {
                        long[] counts = new long[100];
                        for (int tap = 0; tap < 100 * 100; tap++) {
                            long[] v = draws.shuffledNumbers(1, 100, tap);
                            for (int pos = 0; pos < v.length; pos++) {
                                if (v[pos] == 1) {
                                    counts[pos]++;
                                }
                            }
                        }
                        return ChiSquare.uniform("positions of 1 in orders of [1, 100]", counts);
                    });

    static final StatCheck SHUFFLED_NUMBERS_CAPPED =
            new StatCheck(
                    "tenths of [1, 10^12] in capped shuffles",
                    draws -> {
                        long n = 1_000_000_000_000L;
                        long[] counts = new long[10];
                        for (int tap = 0; tap < 200; tap++) {
                            for (long v : draws.shuffledNumbers(1, n, tap)) {
                                counts[(int) ((v - 1) / (n / 10))]++;
                            }
                        }
                        return ChiSquare.uniform("tenths of [1, 10^12] in capped shuffles", counts);
                    });

    // Lists.

    static final StatCheck ITEM_5 =
            new StatCheck(
                    "items drawn from 5",
                    draws -> {
                        List<Integer> items = Arrays.asList(0, 1, 2, 3, 4);
                        long[] counts = new long[5];
                        for (int tap = 0; tap < 5 * 2000; tap++) {
                            counts[draws.item(items, tap)]++;
                        }
                        return ChiSquare.uniform("items drawn from 5", counts);
                    });

    static final StatCheck SHUFFLED_ITEMS_4 =
            new StatCheck(
                    "orders of 4 items",
                    draws -> {
                        List<Integer> items = Arrays.asList(0, 1, 2, 3);
                        long[] counts = new long[256];
                        for (int tap = 0; tap < 24 * 500; tap++) {
                            List<Integer> p = draws.shuffledItems(items, tap);
                            counts[p.get(0) * 64 + p.get(1) * 16 + p.get(2) * 4 + p.get(3)]++;
                        }
                        return ChiSquare.uniform("orders of 4 items", nonEmptyCells(counts, 24));
                    });

    // The first and last items are the ones most affected by off-by-one bugs.
    static final StatCheck SHUFFLED_ITEMS_FIRST = itemPosition("first", 0);
    static final StatCheck SHUFFLED_ITEMS_LAST = itemPosition("last", 19);

    static final List<StatCheck> ALL =
            Arrays.asList(
                    NUMBER_2,
                    NUMBER_6,
                    NUMBER_100,
                    NUMBER_1000,
                    MODULO_BIAS,
                    FULL_RANGE,
                    PAIRS,
                    TRIPLES,
                    RUNS,
                    SHUFFLED_NUMBERS_4,
                    SHUFFLED_NUMBERS_100,
                    SHUFFLED_NUMBERS_CAPPED,
                    ITEM_5,
                    SHUFFLED_ITEMS_4,
                    SHUFFLED_ITEMS_FIRST,
                    SHUFFLED_ITEMS_LAST);

    private static StatCheck numbers(long min, long max, int perValue) {
        String name = "draws from [" + min + ", " + max + "]";
        return new StatCheck(
                name,
                draws -> {
                    int cells = (int) (max - min + 1);
                    long[] counts = new long[cells];
                    for (int tap = 0; tap < cells * perValue; tap++) {
                        counts[(int) (draws.number(min, max, tap) - min)]++;
                    }
                    return ChiSquare.uniform(name, counts);
                });
    }

    /** Positions in shuffled lists of 20 where the given item ends up. */
    private static StatCheck itemPosition(String which, int item) {
        String name = "positions of the " + which + " of 20 items";
        return new StatCheck(
                name,
                draws -> {
                    int n = 20;
                    List<Integer> items = new ArrayList<>();
                    for (int i = 0; i < n; i++) {
                        items.add(i);
                    }
                    long[] counts = new long[n];
                    for (int tap = 0; tap < n * 500; tap++) {
                        counts[draws.shuffledItems(items, tap).indexOf(item)]++;
                    }
                    return ChiSquare.uniform(name, counts);
                });
    }

    /** The nonzero counts, which must correspond exactly to the possible outcomes. */
    private static long[] nonEmptyCells(long[] counts, int expected) {
        long[] result = new long[expected];
        int k = 0;
        for (long c : counts) {
            if (c > 0) {
                assertTrue("more outcomes than possible", k < expected);
                result[k++] = c;
            }
        }
        assertEquals("some outcomes never occurred", expected, k);
        return result;
    }
}
