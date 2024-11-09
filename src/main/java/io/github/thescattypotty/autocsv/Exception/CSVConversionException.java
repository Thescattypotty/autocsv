package io.github.thescattypotty.autocsv.Exception;

public class CSVConversionException extends RuntimeException {
    public CSVConversionException(String message) {
        super(message);
    }

    public CSVConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}