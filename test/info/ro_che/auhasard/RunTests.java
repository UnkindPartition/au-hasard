package info.ro_che.auhasard;

import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/** Runs the tests, printing each test's description and diagnostics as it goes. */
public final class RunTests {
    private static final Class<?> TESTS = RngTest.class;

    public static void main(String[] args) {
        boolean described = true;
        for (Method m : TESTS.getMethods()) {
            if (m.isAnnotationPresent(Test.class) && !m.isAnnotationPresent(About.class)) {
                System.out.println("Test without @About: " + m.getName());
                described = false;
            }
        }
        if (!described) {
            System.exit(1);
        }

        JUnitCore core = new JUnitCore();
        core.addListener(
                new RunListener() {
                    @Override
                    public void testStarted(Description d) {
                        System.out.println();
                        System.out.println(d.getMethodName());
                        printWrapped(d.getAnnotation(About.class).value());
                    }

                    @Override
                    public void testFailure(Failure f) {
                        if (f.getException() instanceof AssertionError) {
                            System.out.println("  FAILED: " + f.getMessage());
                        } else {
                            System.out.println("  FAILED: " + f.getTrace());
                        }
                    }
                });
        Result r = core.run(TESTS);
        System.out.printf(
                "%n%d tests, %d failed, %.1f s%n",
                r.getRunCount(), r.getFailureCount(), r.getRunTime() / 1000.0);
        System.exit(r.wasSuccessful() ? 0 : 1);
    }

    private static void printWrapped(String text) {
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() > 0 && line.length() + 1 + word.length() > 78) {
                System.out.println("  " + line);
                line.setLength(0);
            }
            if (line.length() > 0) {
                line.append(' ');
            }
            line.append(word);
        }
        System.out.println("  " + line);
    }
}
