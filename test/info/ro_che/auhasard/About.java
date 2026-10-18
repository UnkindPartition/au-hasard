package info.ro_che.auhasard;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** What a test checks, printed when it runs. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface About {
    String value();
}
