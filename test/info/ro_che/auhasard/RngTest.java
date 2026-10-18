package info.ro_che.auhasard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

@About("Drawing, picking and shuffling")
public class RngTest {
    private static RandomSource seeded(long seed) {
        return StatCheck.seeded(seed);
    }

    private static void check(StatCheck c, long seed) {
        StatCheck.Outcome o = c.run(seeded(seed));
        System.out.printf("    %s, p = %.3f%n", o.summary, o.p);
        assertTrue(o.summary + ", p = " + o.p, o.p > StatCheck.ALPHA);
    }

    @Test
    @About(
            "Compares the chi-square p-values computed by the test helper with values "
            + "from published tables.")
    public void chiSquarePValue() {
        checkPValue(3.841, 1, 0.05, 1e-4);
        checkPValue(18.307, 10, 0.05, 1e-4);
        checkPValue(29.588, 10, 0.001, 1e-5);
        checkPValue(124.342, 100, 0.05, 1e-4);
        checkPValue(1.386, 2, 0.5, 1e-3);
    }

    private static void checkPValue(double stat, int df, double expected, double tolerance) {
        double p = ChiSquare.pValue(stat, df);
        System.out.printf("    chi-square = %.3f (df %d): p = %.5f, table: %s%n", stat, df, p, expected);
        assertEquals(expected, p, tolerance);
    }

    // Range handling.

    @Test
    @About(
            "Draws 1000 numbers from each of several small ranges, including ranges at "
            + "both ends of the 64-bit integers, and checks that every draw lies within the "
            + "range and every value of the range occurs.")
    public void uniformStaysInRangeAndHitsEveryValue() {
        RandomSource rnd = seeded(1);
        long[][] ranges = {
            {5, 5},
            {-3, 3},
            {0, 1},
            {Long.MIN_VALUE, Long.MIN_VALUE + 4},
            {Long.MAX_VALUE - 4, Long.MAX_VALUE},
        };
        for (long[] r : ranges) {
            Set<Long> seen = new HashSet<>();
            for (int i = 0; i < 1000; i++) {
                long v = Rng.uniform(rnd, r[0], r[1]);
                assertTrue(v >= r[0] && v <= r[1]);
                seen.add(v);
            }
            System.out.printf(
                    "    [%d, %d]: %d of %d values seen in 1000 draws%n",
                    r[0], r[1], seen.size(), r[1] - r[0] + 1);
            assertEquals(r[1] - r[0] + 1, seen.size());
        }
    }

    @Test
    @About(
            "Checks that min > max, a negative count, and asking for more distinct "
            + "numbers or items than exist are all rejected.")
    public void invalidArguments() {
        RandomSource rnd = seeded(2);
        assertThrows(IllegalArgumentException.class, () -> Rng.uniform(rnd, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> Rng.numbers(rnd, 1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> Rng.numbers(rnd, 0, 1, -1));
        assertThrows(IllegalArgumentException.class, () -> Rng.distinctNumbers(rnd, 1, 3, 4));
        assertThrows(IllegalArgumentException.class, () -> Rng.distinctNumbers(rnd, 0, 1, -1));
        assertThrows(
                IllegalArgumentException.class, () -> Rng.pick(rnd, Arrays.asList(1, 2), 3));
        assertThrows(
                IllegalArgumentException.class, () -> Rng.pick(rnd, Arrays.asList(1, 2), -1));
    }

    @Test
    @About(
            "Checks that asking for zero numbers or items, or shuffling an empty list, "
            + "gives an empty result.")
    public void emptyResults() {
        RandomSource rnd = seeded(3);
        assertEquals(0, Rng.numbers(rnd, 0, 9, 0).length);
        assertEquals(0, Rng.distinctNumbers(rnd, 0, 9, 0).length);
        assertTrue(Rng.pick(rnd, Arrays.asList(1, 2), 0).isEmpty());
        assertTrue(Rng.shuffle(rnd, new ArrayList<Integer>()).isEmpty());
    }

    // Distribution.

    @Test
    @About(
            "For ranges of 2, 6, 100 and 1000 values, draws respectively 50000, 10000, "
            + "1000 and 1000 numbers per value and checks with a chi-square test that all "
            + "values are equally frequent.")
    public void uniformSmallRanges() {
        check(StatCheck.UNIFORM_2, 10);
        check(StatCheck.UNIFORM_6, 11);
        check(StatCheck.UNIFORM_100, 12);
        check(StatCheck.UNIFORM_1000, 13);
    }

    @Test
    @About(
            "Draws from a range of 3 * 2^62 values. Reducing a random 64-bit word modulo "
            + "the range size would make the lowest third of the range twice as likely as "
            + "each of the other two; checks with a chi-square test that the three thirds "
            + "are equally frequent.")
    public void uniformHasNoModuloBias() {
        check(StatCheck.MODULO_BIAS, 14);
    }

    @Test
    @About(
            "Draws from the full 64-bit range and checks with a chi-square test that the "
            + "top 4 bits of the results are equally likely to take each of their 16 "
            + "values.")
    public void uniformFullRange() {
        check(StatCheck.FULL_RANGE, 15);
    }

    @Test
    @About(
            "Draws 10000 numbers from 1..100 using the app's generator, keyed from "
            + "/dev/urandom, and checks that every value occurs. The key is unpredictable, so "
            + "this is only a coarse check.")
    public void uniformWithKernelKey() {
        RandomSource rnd = ChaCha20Source.fromKernel();
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < 10000; i++) {
            seen.add(Rng.uniform(rnd, 1, 100));
        }
        System.out.printf("    [1, 100]: %d of 100 values seen in 10000 draws%n", seen.size());
        assertEquals(100, seen.size());
    }

    // Independence.

    @Test
    @About(
            "Draws 36000 pairs of numbers from 0..5 and checks with a chi-square test "
            + "that all 36 combinations are equally frequent, i.e. the second number "
            + "doesn't depend on the first.")
    public void consecutivePairsAreIndependent() {
        check(StatCheck.PAIRS, 20);
    }

    @Test
    @About(
            "Draws a sequence of numbers from 0..3, splits it into consecutive triples, "
            + "and checks with a chi-square test that all 64 possible triples are equally "
            + "frequent.")
    public void consecutiveTriplesAreIndependent() {
        check(StatCheck.TRIPLES, 21);
    }

    @Test
    @About(
            "Draws 100000 numbers and counts the runs of consecutive increases and "
            + "decreases. Too few runs would indicate trends, too many would indicate "
            + "alternation; checks that the count is within 3.3 standard deviations (p > "
            + "0.001) of the expected count for independent draws.")
    public void runsUpAndDown() {
        check(StatCheck.RUNS, 22);
    }

    // Distinct numbers.

    @Test
    @About(
            "For several cases (all values of a small range, a few values of a huge "
            + "range, ranges at the ends of the 64-bit integers), draws distinct numbers "
            + "100 times and checks that each result has the requested size, lies within "
            + "the range and has no repeats.")
    public void distinctNumbersAreDistinctAndInRange() {
        RandomSource rnd = seeded(30);
        long[][] cases = {
            {1, 10, 10},
            {1, 10, 3},
            {-5, 5, 11},
            {Long.MAX_VALUE - 9, Long.MAX_VALUE, 10},
            {Long.MIN_VALUE, Long.MIN_VALUE + 9, 10},
            {Long.MIN_VALUE, Long.MAX_VALUE, 1000},
            {1, 1_000_000_000_000L, 1000},
        };
        for (long[] c : cases) {
            long[] v = null;
            for (int rep = 0; rep < 100; rep++) {
                v = Rng.distinctNumbers(rnd, c[0], c[1], (int) c[2]);
                assertEquals(c[2], v.length);
                Set<Long> seen = new HashSet<>();
                for (long x : v) {
                    assertTrue(x >= c[0] && x <= c[1]);
                    assertTrue(seen.add(x));
                }
            }
            System.out.printf(
                    "    %d of [%d, %d], 100 times; last: %s%n",
                    c[2], c[0], c[1], c[2] <= 11 ? Arrays.toString(v) : "(too long)");
        }
    }

    @Test
    @About(
            "Draws 3 distinct numbers from 0..4, 60000 times. There are 5 * 4 * 3 = 60 "
            + "possible ordered results; checks that exactly these occur and, with a "
            + "chi-square test, that they are equally frequent. This covers both which numbers "
            + "are chosen and their order.")
    public void distinctNumbersOrderedSelectionsUniform() {
        check(StatCheck.DISTINCT_3_OF_5, 31);
    }

    @Test
    @About(
            "Draws pairs of distinct numbers from the full 64-bit range and checks with a "
            + "chi-square test that the top 4 bits of the results are equally likely to "
            + "take each of their 16 values.")
    public void distinctNumbersFullRangeUniform() {
        check(StatCheck.DISTINCT_FULL_RANGE, 32);
    }

    // List picks.

    @Test
    @About(
            "Picks 3 items from a list of 5, 60000 times. Checks that all 60 possible "
            + "ordered picks occur, and no others, and with a chi-square test that they are "
            + "equally frequent.")
    public void pickOrderedSelectionsUniform() {
        check(StatCheck.PICK_3_OF_5, 40);
    }

    @Test
    @About(
            "Checks that picking from and shuffling a list leave the list itself "
            + "unchanged.")
    public void pickDoesNotModifyInput() {
        RandomSource rnd = seeded(41);
        List<Integer> items = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        List<Integer> copy = new ArrayList<>(items);
        Rng.pick(rnd, items, 3);
        Rng.shuffle(rnd, items);
        assertEquals(copy, items);
    }

    @Test
    @About(
            "Shuffles a list of 4 items 24000 times. Checks that all 24 possible "
            + "orderings occur, and no others, and with a chi-square test that they are "
            + "equally frequent.")
    public void shuffleAllPermutationsUniform() {
        check(StatCheck.SHUFFLE_4, 42);
    }

    @Test
    @About(
            "Shuffles a list of 20 items 20000 times and records where the first item "
            + "ends up; then does the same for the last item. Checks with chi-square tests "
            + "that each is equally likely to land in any of the 20 positions.")
    public void shufflePositionsUniform() {
        check(StatCheck.SHUFFLE_FIRST, 43);
        check(StatCheck.SHUFFLE_LAST, 45);
    }

    @Test
    @About(
            "Shuffles a list of 50 items and checks that the result contains the same "
            + "items, not in the original order.")
    public void shuffleIsPermutation() {
        RandomSource rnd = seeded(44);
        List<Integer> items = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            items.add(i);
        }
        List<Integer> p = Rng.shuffle(rnd, items);
        System.out.println("    " + p);
        assertNotEquals(items, p);
        assertEquals(new HashSet<>(items), new HashSet<>(p));
    }
}
