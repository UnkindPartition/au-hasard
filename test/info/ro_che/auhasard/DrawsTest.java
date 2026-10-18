package info.ro_che.auhasard;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

@About("The app's draws: statistics")
public class DrawsTest {
    private static void check(StatCheck c, long seed) {
        StatCheck.Outcome o = c.run(StatCheck.seeded(seed));
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

    // Numbers.

    @Test
    @About(
            "For ranges of 2, 6, 100 and 1000 values, draws respectively 10000, 2000, 200 "
            + "and 50 numbers per value, each as a separate tap, and checks with a chi-square "
            + "test that all values are equally frequent.")
    public void numbersUniform() {
        check(StatCheck.NUMBER_2, 10);
        check(StatCheck.NUMBER_6, 11);
        check(StatCheck.NUMBER_100, 12);
        check(StatCheck.NUMBER_1000, 13);
    }

    @Test
    @About(
            "Draws from a range of 3 * 2^62 values. Reducing a random 64-bit word modulo "
            + "the range size would make the lowest third of the range twice as likely as "
            + "each of the other two; checks with a chi-square test that the three thirds "
            + "are equally frequent.")
    public void numbersHaveNoModuloBias() {
        check(StatCheck.MODULO_BIAS, 14);
    }

    @Test
    @About(
            "Draws from the full 64-bit range and checks with a chi-square test that the "
            + "top 4 bits of the results are equally likely to take each of their 16 "
            + "values.")
    public void numbersFullRange() {
        check(StatCheck.FULL_RANGE, 15);
    }

    @Test
    @About(
            "Draws 18000 pairs of numbers from 0..5, each number as a separate tap, and checks "
            + "with a chi-square test that all 36 combinations are equally frequent, i.e. a "
            + "draw doesn't depend on the previous one.")
    public void consecutiveDrawsIndependent() {
        check(StatCheck.PAIRS, 20);
    }

    @Test
    @About(
            "Draws 12800 triples of numbers from 0..3, each number as a separate tap, and "
            + "checks with a chi-square test that all 64 combinations are equally frequent.")
    public void consecutiveTriplesIndependent() {
        check(StatCheck.TRIPLES, 21);
    }

    @Test
    @About(
            "Draws 20000 numbers, each as a separate tap, and counts the runs of consecutive "
            + "increases and decreases. Too few runs would indicate trends, too many would "
            + "indicate alternation; checks that the count is within 3.3 standard deviations "
            + "(p > 0.001) of the expected count for independent draws.")
    public void runsUpAndDown() {
        check(StatCheck.RUNS, 22);
    }

    // Shuffled numbers.

    @Test
    @About(
            "Shuffles the numbers 1..4 12000 times. Checks that all 24 possible orders occur, "
            + "and no others, and with a chi-square test that they are equally frequent.")
    public void shuffledNumbersOrdersUniform() {
        check(StatCheck.SHUFFLED_NUMBERS_4, 30);
    }

    @Test
    @About(
            "Shuffles the numbers 1..100, the largest range shuffled whole, 10000 times, and "
            + "checks with a chi-square test that 1 is equally likely to land in any of the "
            + "100 positions.")
    public void shuffledNumbersPositionsUniform() {
        check(StatCheck.SHUFFLED_NUMBERS_100, 31);
    }

    @Test
    @About(
            "Shuffles the numbers 1..10^12, of which only 100 are shown, 200 times, and checks "
            + "with a chi-square test that the numbers shown are equally likely to come from "
            + "each tenth of the range.")
    public void cappedShuffledNumbersUniform() {
        check(StatCheck.SHUFFLED_NUMBERS_CAPPED, 32);
    }

    @Test
    @About(
            "Checks that shuffling numbers gives all numbers of ranges of up to 100, and 100 "
            + "different ones of larger ranges, up to the full 64-bit range, all within the "
            + "range.")
    public void shuffledNumbersCountsAndRanges() {
        Draws draws = StatCheck.seeded(33);
        long[][] cases = {
            {7, 7, 1},
            {1, 100, 100},
            {1, 101, 100},
            {-50, 49, 100},
            {Long.MAX_VALUE - 99, Long.MAX_VALUE, 100},
            {Long.MIN_VALUE, Long.MAX_VALUE, 100},
        };
        for (long[] c : cases) {
            long[] v = draws.shuffledNumbers(c[0], c[1], 0);
            assertEquals(c[2], v.length);
            Set<Long> seen = new HashSet<>();
            for (long x : v) {
                assertTrue(x >= c[0] && x <= c[1]);
                assertTrue(seen.add(x));
            }
            System.out.printf("    [%d, %d]: %d numbers%n", c[0], c[1], v.length);
        }
    }

    // Lists.

    @Test
    @About(
            "Draws an item from a list of 5, 10000 times, each as a separate tap, and checks "
            + "with a chi-square test that all items are equally frequent.")
    public void itemsUniform() {
        check(StatCheck.ITEM_5, 40);
    }

    @Test
    @About(
            "Shuffles a list of 4 items 12000 times. Checks that all 24 possible orders occur, "
            + "and no others, and with a chi-square test that they are equally frequent.")
    public void shuffledItemsOrdersUniform() {
        check(StatCheck.SHUFFLED_ITEMS_4, 41);
    }

    @Test
    @About(
            "Shuffles a list of 20 items 10000 times and records where the first item ends "
            + "up; then does the same for the last item. Checks with chi-square tests that "
            + "each is equally likely to land in any of the 20 positions.")
    public void shuffledItemsPositionsUniform() {
        check(StatCheck.SHUFFLED_ITEMS_FIRST, 42);
        check(StatCheck.SHUFFLED_ITEMS_LAST, 43);
    }

    // The app's generator.

    @Test
    @About(
            "Checks that the values shown by the rolling animation between draws don't change "
            + "the draws themselves.")
    public void reelDoesNotAffectDraws() {
        Draws plain = StatCheck.seeded(50);
        Draws withReel = StatCheck.seeded(50);
        long[] a = new long[100];
        long[] b = new long[100];
        for (int tap = 0; tap < 100; tap++) {
            a[tap] = plain.number(1, 1000, tap);
            b[tap] = withReel.number(1, 1000, tap);
            for (int i = 0; i < 7; i++) {
                withReel.reelNumber(1, 1000);
            }
        }
        assertArrayEquals(a, b);
    }

    @Test
    @About(
            "Draws 10000 numbers from 1..100 with the app's generator keyed from /dev/urandom, "
            + "and checks that every value occurs. The key is unpredictable, so this is only a "
            + "coarse check.")
    public void kernelKeyedDraws() {
        Draws draws = Draws.fromKernel();
        Set<Long> seen = new HashSet<>();
        for (int tap = 0; tap < 10000; tap++) {
            seen.add(draws.number(1, 100, tap));
        }
        System.out.printf("    [1, 100]: %d of 100 values seen in 10000 draws%n", seen.size());
        assertEquals(100, seen.size());
    }
}
