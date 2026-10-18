package info.ro_che.auhasard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;
import java.util.function.Function;

/** A statistical check of {@link Rng}: draws from a random source and computes a p-value. */
final class StatCheck {
    /** Significance level. */
    static final double ALPHA = 1e-3;

    /**
     * Draws per cell of a chi-square test. Tests with few cells use more, since otherwise
     * their p-values take too few distinct values to be close to uniform.
     */
    private static final int PER_CELL = 1000;
    private static final int PER_CELL_FEW = 10000;

    static final class Outcome {
        final String summary;
        final double p;

        Outcome(String summary, double p) {
            this.summary = summary;
            this.p = p;
        }
    }

    final String name;
    private final Function<RandomSource, Outcome> run;

    private StatCheck(String name, Function<RandomSource, Outcome> run) {
        this.name = name;
        this.run = run;
    }

    Outcome run(RandomSource rnd) {
        return run.apply(rnd);
    }

    static RandomSource seeded(long seed) {
        return new SplittableRandom(seed)::nextLong;
    }

    static final StatCheck UNIFORM_2 = uniform(0, 1, 5 * PER_CELL_FEW);
    static final StatCheck UNIFORM_6 = uniform(1, 6, PER_CELL_FEW);
    static final StatCheck UNIFORM_100 = uniform(-50, 49, PER_CELL);
    static final StatCheck UNIFORM_1000 = uniform(1, 1000, PER_CELL);

    // With plain x mod n, the first third of this range would come up twice as often as
    // each of the other two thirds.
    static final StatCheck MODULO_BIAS =
            new StatCheck(
                    "thirds of [-2^63, 2^62)",
                    rnd -> {
                        long min = Long.MIN_VALUE;
                        long max = (1L << 62) - 1;
                        long[] counts = new long[3];
                        for (int i = 0; i < 3 * PER_CELL_FEW; i++) {
                            counts[(int) ((Rng.uniform(rnd, min, max) - min) >>> 62)]++;
                        }
                        return ChiSquare.uniform("thirds of [-2^63, 2^62)", counts);
                    });

    static final StatCheck FULL_RANGE =
            new StatCheck(
                    "top 4 bits of the full range",
                    rnd -> {
                        long[] counts = new long[16];
                        for (int i = 0; i < 16 * PER_CELL; i++) {
                            long v = Rng.uniform(rnd, Long.MIN_VALUE, Long.MAX_VALUE);
                            counts[(int) (v >>> 60)]++;
                        }
                        return ChiSquare.uniform("top 4 bits of the full range", counts);
                    });

    static final StatCheck PAIRS =
            new StatCheck(
                    "pairs from [0, 5]",
                    rnd -> {
                        long[] counts = new long[36];
                        for (int i = 0; i < 36 * PER_CELL; i++) {
                            long[] v = Rng.numbers(rnd, 0, 5, 2);
                            counts[(int) (v[0] * 6 + v[1])]++;
                        }
                        return ChiSquare.uniform("pairs from [0, 5]", counts);
                    });

    static final StatCheck TRIPLES =
            new StatCheck(
                    "triples from [0, 3]",
                    rnd -> {
                        long[] counts = new long[64];
                        long[] v = Rng.numbers(rnd, 0, 3, 3 * 64 * PER_CELL);
                        for (int i = 0; i < v.length; i += 3) {
                            counts[(int) (v[i] * 16 + v[i + 1] * 4 + v[i + 2])]++;
                        }
                        return ChiSquare.uniform("triples from [0, 3]", counts);
                    });

    // Wald-Wolfowitz: the number of runs of increases and decreases in n continuous
    // i.i.d. values has mean (2n - 1) / 3 and variance (16n - 29) / 90.
    static final StatCheck RUNS =
            new StatCheck(
                    "runs up and down",
                    rnd -> {
                        int n = 100000;
                        long[] v = Rng.numbers(rnd, Long.MIN_VALUE, Long.MAX_VALUE, n);
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

    // 3 of 5 values: 5 * 4 * 3 = 60 ordered selections.
    static final StatCheck DISTINCT_3_OF_5 =
            new StatCheck(
                    "ordered 3 of [0, 4]",
                    rnd -> {
                        long[] counts = new long[125];
                        for (int i = 0; i < 60 * PER_CELL; i++) {
                            long[] v = Rng.distinctNumbers(rnd, 0, 4, 3);
                            counts[(int) (v[0] * 25 + v[1] * 5 + v[2])]++;
                        }
                        return ChiSquare.uniform("ordered 3 of [0, 4]", nonEmptyCells(counts, 60));
                    });

    static final StatCheck DISTINCT_FULL_RANGE =
            new StatCheck(
                    "top 4 bits of distinct numbers",
                    rnd -> {
                        long[] counts = new long[16];
                        for (int i = 0; i < 16 * PER_CELL / 2; i++) {
                            for (long x :
                                    Rng.distinctNumbers(rnd, Long.MIN_VALUE, Long.MAX_VALUE, 2)) {
                                counts[(int) (x >>> 60)]++;
                            }
                        }
                        return ChiSquare.uniform("top 4 bits of distinct numbers", counts);
                    });

    static final StatCheck PICK_3_OF_5 =
            new StatCheck(
                    "ordered 3 of 5 items",
                    rnd -> {
                        List<Integer> items = Arrays.asList(0, 1, 2, 3, 4);
                        long[] counts = new long[125];
                        for (int i = 0; i < 60 * PER_CELL; i++) {
                            List<Integer> p = Rng.pick(rnd, items, 3);
                            counts[p.get(0) * 25 + p.get(1) * 5 + p.get(2)]++;
                        }
                        return ChiSquare.uniform("ordered 3 of 5 items", nonEmptyCells(counts, 60));
                    });

    static final StatCheck SHUFFLE_4 =
            new StatCheck(
                    "permutations of 4 items",
                    rnd -> {
                        List<Integer> items = Arrays.asList(0, 1, 2, 3);
                        long[] counts = new long[256];
                        for (int i = 0; i < 24 * PER_CELL; i++) {
                            List<Integer> p = Rng.shuffle(rnd, items);
                            counts[p.get(0) * 64 + p.get(1) * 16 + p.get(2) * 4 + p.get(3)]++;
                        }
                        return ChiSquare.uniform(
                                "permutations of 4 items", nonEmptyCells(counts, 24));
                    });

    // The first and last items are the ones most affected by off-by-one bugs.
    static final StatCheck SHUFFLE_FIRST = shufflePosition("first", 0);
    static final StatCheck SHUFFLE_LAST = shufflePosition("last", 19);

    static final List<StatCheck> ALL =
            Arrays.asList(
                    UNIFORM_2,
                    UNIFORM_6,
                    UNIFORM_100,
                    UNIFORM_1000,
                    MODULO_BIAS,
                    FULL_RANGE,
                    PAIRS,
                    TRIPLES,
                    RUNS,
                    DISTINCT_3_OF_5,
                    DISTINCT_FULL_RANGE,
                    PICK_3_OF_5,
                    SHUFFLE_4,
                    SHUFFLE_FIRST,
                    SHUFFLE_LAST);

    private static StatCheck uniform(long min, long max, int perCell) {
        String name = "[" + min + ", " + max + "]";
        return new StatCheck(
                name,
                rnd -> {
                    int cells = (int) (max - min + 1);
                    long[] counts = new long[cells];
                    for (int i = 0; i < cells * perCell; i++) {
                        counts[(int) (Rng.uniform(rnd, min, max) - min)]++;
                    }
                    return ChiSquare.uniform(name, counts);
                });
    }

    /** Positions in a shuffled list of 20 where the given item ends up. */
    private static StatCheck shufflePosition(String which, int item) {
        String name = "positions of the " + which + " of 20 items";
        return new StatCheck(
                name,
                rnd -> {
                    int n = 20;
                    List<Integer> items = new ArrayList<>();
                    for (int i = 0; i < n; i++) {
                        items.add(i);
                    }
                    long[] counts = new long[n];
                    for (int i = 0; i < n * PER_CELL; i++) {
                        counts[Rng.shuffle(rnd, items).indexOf(item)]++;
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
