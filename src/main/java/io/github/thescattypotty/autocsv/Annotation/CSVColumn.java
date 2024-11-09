package io.github.thescattypotty.autocsv.Annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify properties of a field to be mapped to a CSV column.
 * <p>
 * This annotation can be used on class fields to configure CSV serialization
 * and deserialization behavior.
 * You can specify the name of the CSV column, whether the field is required,
 * date or number formatting,
 * and whether the field should be ignored during CSV processing.
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * {
 *     &#64;code
 *     public class Person {
 *         &#64;CSVColumn(name = "First Name", required = true)
 *         private String firstName;
 * 
 *         &#64;CSVColumn(name = "Birth Date", dateFormat = "dd/MM/yyyy")
 *         private LocalDate birthDate;
 * 
 *         @CSVColumn(ignore = true)
 *         private String internalId;
 *     }
 * }
 * </pre>
 * 
 * <p>
 * This will map the fields to CSV columns, specifying the name and format where
 * necessary.
 * </p>
 * 
 * @author Thescattypotty
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CSVColumn {
    /**
     * Specifies the name of the CSV column.
     * If not provided, the field name will be used by default.
     * 
     * @return The name of the CSV column.
     */
    String name() default "";

    /**
     * Marks the field as required in the CSV.
     * If true, this field must have a value during deserialization and cannot be
     * null or empty.
     * 
     * @return true if the field is required, false otherwise.
     */
    boolean required() default false;

    /**
     * Specifies the date format to be used for date fields.
     * The format should follow the {@code SimpleDateFormat} pattern.
     * 
     * @return The date format pattern, default is "yyyy-MM-dd".
     */
    String dateFormat() default "yyyy-MM-dd";

    /**
     * Specifies the number format to be used for numeric fields.
     * This should follow the {@code DecimalFormat} pattern.
     * 
     * @return The number format pattern, empty string by default.
     */
    String numberFormat() default "";

    /**
     * Specifies whether the field should be ignored during CSV processing.
     * If true, the field will not be included in the CSV serialization or
     * deserialization.
     * 
     * @return true if the field should be ignored, false otherwise.
     */
    boolean ignore() default false;
}
