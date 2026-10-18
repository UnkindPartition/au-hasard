package info.ro_che.auhasard;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Runs each statistical check with many seeds and tests whether its p-values are uniformly
 * distributed (Kolmogorov-Smirnov). Writes the p-values to build/stats/ for plotting.
 */
public final class Stats {
    private static final int SEEDS = 2000;
    private static final String POOLED = "all checks pooled";

    public static void main(String[] args) throws IOException {
        Files.createDirectories(Paths.get("build/stats"));
        boolean ok = true;
        List<Double> pooled = new ArrayList<>();
        try (PrintWriter pvalues = new PrintWriter("build/stats/pvalues.csv");
                PrintWriter ks = new PrintWriter("build/stats/ks.csv")) {
            pvalues.println("check,seed,p");
            ks.println("check,D,p");
            for (int k = 0; k < StatCheck.ALL.size(); k++) {
                StatCheck c = StatCheck.ALL.get(k);
                double[] ps = new double[SEEDS];
                try {
                    for (int i = 0; i < SEEDS; i++) {
                        // Distinct seeds for each check, so that checks are independent.
                        long seed = ((long) k << 32) | i;
                        ps[i] = c.run(StatCheck.seeded(seed)).p;
                        pvalues.printf("\"%s\",%d,%s%n", c.name, seed, ps[i]);
                        pooled.add(ps[i]);
                    }
                } catch (AssertionError e) {
                    System.out.printf("%-36s FAILED: %s%n", c.name, e.getMessage());
                    ok = false;
                    continue;
                }
                ok &= report(c.name, ps, ks);
            }
            double[] all = new double[pooled.size()];
            for (int i = 0; i < all.length; i++) {
                all[i] = pooled.get(i);
            }
            System.out.println();
            ok &= report(POOLED, all, ks);
        }
        System.exit(ok ? 0 : 1);
    }

    private static boolean report(String name, double[] ps, PrintWriter ks) {
        double[] sorted = ps.clone();
        Arrays.sort(sorted);
        int n = sorted.length;
        double d = 0;
        for (int i = 0; i < n; i++) {
            d = Math.max(d, Math.max((i + 1.0) / n - sorted[i], sorted[i] - (double) i / n));
        }
        double p = ksPValue(d, n);
        boolean ok = p > StatCheck.ALPHA;
        System.out.printf(
                "%-36s %d p-values, min %.4f, KS D = %.3f, p = %.3f%s%n",
                name, n, sorted[0], d, p, ok ? "" : "  FAILED");
        ks.printf("\"%s\",%s,%s%n", name, d, p);
        return ok;
    }

    /** Asymptotic Kolmogorov distribution (Numerical Recipes, 14.3). */
    private static double ksPValue(double d, int n) {
        double en = Math.sqrt(n);
        double lambda = (en + 0.12 + 0.11 / en) * d;
        double a2 = -2 * lambda * lambda;
        double fac = 2;
        double sum = 0;
        double prev = 0;
        for (int j = 1; j <= 100; j++) {
            double term = fac * Math.exp(a2 * j * j);
            sum += term;
            if (Math.abs(term) <= 1e-3 * prev || Math.abs(term) <= 1e-8 * sum) {
                return Math.min(1, Math.max(0, sum));
            }
            fac = -fac;
            prev = Math.abs(term);
        }
        return 1;
    }
}
