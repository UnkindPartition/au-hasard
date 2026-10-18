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

@About("Sampling methods: exact properties")
public class RngTest {
    private static RandomSource seeded(long seed) {
        return StatCheck.generator(seed);
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

    // Distinct numbers and lists.

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
