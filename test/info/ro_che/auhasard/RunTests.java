package info.ro_che.auhasard;

import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.PrintStream;
import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * Runs the tests and prints them as a tree, in the style of Haskell's Tasty: a group per test
 * class, each test's result aligned after its name, then its description and anything it
 * printed.
 */
public final class RunTests {
    private static final Class<?>[] TESTS = {ChaCha20Test.class, RngTest.class};

    private static final PrintStream out = System.out;
    private static final boolean color = isTerminal();

    public static void main(String[] args) {
        if (!allDescribed()) {
            System.exit(1);
        }
        int run = 0;
        int failed = 0;
        long start = System.nanoTime();
        for (Class<?> c : TESTS) {
            out.println(c.getAnnotation(About.class).value());
            Result r = runGroup(c);
            run += r.getRunCount();
            failed += r.getFailureCount();
        }
        String time = seconds(System.nanoTime() - start);
        out.println();
        out.println(
                failed == 0
                        ? paint("All " + run + " tests passed (" + time + ")", "32")
                        : paint(failed + " out of " + run + " tests failed (" + time + ")", "31"));
        System.exit(failed == 0 ? 0 : 1);
    }

    private static Result runGroup(Class<?> c) {
        int width = 0;
        for (Description d : Request.aClass(c).getRunner().getDescription().getChildren()) {
            width = Math.max(width, d.getMethodName().length());
        }
        int nameWidth = width;
        JUnitCore core = new JUnitCore();
        core.addListener(
                new RunListener() {
                    private final ByteArrayOutputStream captured = new ByteArrayOutputStream();
                    private Failure failure;
                    private long start;

                    @Override
                    public void testStarted(Description d) {
                        captured.reset();
                        failure = null;
                        System.setOut(new PrintStream(captured, true));
                        start = System.nanoTime();
                    }

                    @Override
                    public void testFailure(Failure f) {
                        failure = f;
                    }

                    @Override
                    public void testFinished(Description d) {
                        long elapsed = System.nanoTime() - start;
                        System.setOut(out);
                        String name = d.getMethodName() + ":";
                        String result = failure == null ? paint("OK", "32") : paint("FAIL", "31");
                        // Like Tasty, show the time only when it's noticeable.
                        String time = elapsed >= 10_000_000 ? " (" + seconds(elapsed) + ")" : "";
                        out.printf("  %-" + (nameWidth + 1) + "s %s%s%n", name, result, time);
                        printWrapped(d.getAnnotation(About.class).value(), "2");
                        out.print(captured.toString());
                        if (failure != null) {
                            String message =
                                    failure.getException() instanceof AssertionError
                                            ? failure.getMessage()
                                            : failure.getTrace();
                            for (String line : String.valueOf(message).split("\n")) {
                                out.println("    " + paint(line, "31"));
                            }
                        }
                    }
                });
        return core.run(c);
    }

    private static boolean allDescribed() {
        boolean described = true;
        for (Class<?> c : TESTS) {
            if (!c.isAnnotationPresent(About.class)) {
                out.println("Test class without @About: " + c.getSimpleName());
                described = false;
            }
            for (Method m : c.getMethods()) {
                if (m.isAnnotationPresent(Test.class) && !m.isAnnotationPresent(About.class)) {
                    out.println("Test without @About: " + m.getName());
                    described = false;
                }
            }
        }
        return described;
    }

    /** Wraps text at 78 columns, indented by 4, in the given style. */
    private static void printWrapped(String text, String sgr) {
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() > 0 && line.length() + 1 + word.length() > 74) {
                out.println("    " + paint(line.toString(), sgr));
                line.setLength(0);
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(word);
        }
        out.println("    " + paint(line.toString(), sgr));
    }

    /** Text in an ANSI color or style, when printing to a terminal. */
    private static String paint(String text, String sgr) {
        return color ? "\033[" + sgr + "m" + text + "\033[0m" : text;
    }

    private static String seconds(long nanos) {
        return String.format("%.2fs", nanos / 1e9);
    }

    private static boolean isTerminal() {
        Console console = System.console();
        if (console == null) {
            return false;
        }
        // Since Java 22, there's a console even when output is redirected; ask it.
        try {
            return (Boolean) Console.class.getMethod("isTerminal").invoke(console);
        } catch (ReflectiveOperationException e) {
            return true;
        }
    }
}
