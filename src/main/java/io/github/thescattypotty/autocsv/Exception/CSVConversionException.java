package io.github.thescattypotty.autocsv.Exception;

/**
 * Exception thrown when there is an error during the conversion between
 * a CSV and a Java object or vice versa.
 * <p>
 * This is a custom runtime exception that can be thrown in situations where
 * conversion fails, typically during CSV serialization or deserialization.
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * {@code
 * if (conversionFailed) {
 *     throw new CSVConversionException("Conversion failed due to invalid format.");
 * }
 * }
 * </pre>
 * 
 * @author Thescattypotty
 */
public class CSVConversionException extends RuntimeException {

    /**
     * Constructs a new CSVConversionException with the specified detail message.
     * 
     * @param message the detail message explaining the cause of the exception.
     */
    public CSVConversionException(String message) {
        super(message);
    }

    /**
     * Constructs a new CSVConversionException with the specified detail message and
     * cause.
     * 
     * @param message the detail message explaining the cause of the exception.
     * @param cause   the cause of the exception (a {@code Throwable} that led to
     *                this exception).
     */
    public CSVConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}