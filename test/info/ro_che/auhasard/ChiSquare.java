package info.ro_che.auhasard;

/** Pearson's chi-square goodness-of-fit test against a uniform distribution. */
final class ChiSquare {
    private ChiSquare() {}

    static StatCheck.Outcome uniform(String what, long[] counts) {
        long total = 0;
        for (long c : counts) {
            total += c;
        }
        double expected = (double) total / counts.length;
        double stat = 0;
        for (long c : counts) {
            stat += (c - expected) * (c - expected) / expected;
        }
        int df = counts.length - 1;
        return new StatCheck.Outcome(
                String.format(
                        "%s: %d draws in %d cells, chi-square = %.1f (df %d)",
                        what, total, counts.length, stat, df),
                pValue(stat, df));
    }

    /** Upper tail of the chi-square distribution. */
    static double pValue(double stat, int df) {
        return gammaQ(df / 2.0, stat / 2.0);
    }

    // Regularized upper incomplete gamma function (Numerical Recipes, 6.2).
    private static double gammaQ(double a, double x) {
        if (x <= 0) {
            return 1;
        }
        double prefix = Math.exp(-x + a * Math.log(x) - lnGamma(a));
        if (x < a + 1) {
            double ap = a;
            double del = 1 / a;
            double sum = del;
            for (int n = 0; n < 10000 && Math.abs(del) > Math.abs(sum) * 1e-15; n++) {
                ap += 1;
                del *= x / ap;
                sum += del;
            }
            return 1 - sum * prefix;
        }
        final double tiny = 1e-300;
        double b = x + 1 - a;
        double c = 1 / tiny;
        double d = 1 / b;
        double h = d;
        for (int i = 1; i < 10000; i++) {
            double an = -i * (i - a);
            b += 2;
            d = an * d + b;
            if (Math.abs(d) < tiny) {
                d = tiny;
            }
            c = b + an / c;
            if (Math.abs(c) < tiny) {
                c = tiny;
            }
            d = 1 / d;
            double del = d * c;
            h *= del;
            if (Math.abs(del - 1) < 1e-15) {
                break;
            }
        }
        return prefix * h;
    }

    // Lanczos approximation.
    private static double lnGamma(double x) {
        final double[] cof = {
            76.18009172947146, -86.50532032941677, 24.01409824083091,
            -1.231739572450155, 0.1208650973866179e-2, -0.5395239384953e-5,
        };
        double y = x;
        double tmp = x + 5.5;
        tmp -= (x + 0.5) * Math.log(tmp);
        double ser = 1.000000000190015;
        for (double c : cof) {
            ser += c / ++y;
        }
        return -tmp + Math.log(2.5066282746310005 * ser / x);
    }
}
