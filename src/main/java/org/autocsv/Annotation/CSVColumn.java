package org.autocsv.Annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CSVColumn {
    String name() default "";

    boolean required() default false;

    String dateFormat() default "yyyy-MM-dd";

    String numberFormat() default "";

    boolean ignore() default false;
}
