package io.github.thescattypotty.autocsv.Service;

import com.opencsv.CSVWriter;

/**
 * A class representing configurable options for CSV writing.
 * <p>
 * This class allows customization of CSV output, such as setting the separator,
 * quote character, escape character, and line ending. It provides a fluent API
 * for easy configuration.
 * </p>
 * 
 * <p>
 * Example usage:
 * </p>
 * 
 * <pre>
 * {@code
 * CSVOptions options = CSVOptions.defaultOptions()
 *         .withSeparator(',')
 *         .withQuoteChar('"');
 * }
 * </pre>
 * 
 * @see com.opencsv.CSVWriter
 * @author Thescattypotty
 */
public class CSVOptions {
    private char separator = CSVWriter.DEFAULT_SEPARATOR;
    private char quoteChar = CSVWriter.NO_QUOTE_CHARACTER;
    private char escapeChar = CSVWriter.NO_ESCAPE_CHARACTER;
    private String lineEnd = CSVWriter.DEFAULT_LINE_END;

    /**
     * Creates a new instance of CSVOptions with default values.
     * <p>
     * By default, the separator is a comma (','), no quote character is used,
     * no escape character is used, and the line ending is the system default.
     * </p>
     * 
     * @return a new instance of CSVOptions with default settings.
     */
    public static CSVOptions defaultOptions() {
        return new CSVOptions();
    }

    /**
     * Sets a custom separator for CSV fields.
     * <p>
     * The separator is the character used to separate fields in the CSV.
     * </p>
     * 
     * @param separator the character to use as a field separator (e.g., ',' or
     *                  ';').
     * @return this CSVOptions instance for method chaining.
     */
    public CSVOptions withSeparator(char separator) {
        this.separator = separator;
        return this;
    }

    /**
     * Sets a custom quote character for CSV fields.
     * <p>
     * The quote character is used to enclose fields that contain special characters
     * (such as the separator or newlines). By default, no quote character is used.
     * </p>
     * 
     * @param quoteChar the character to use as a quote (e.g., '"').
     * @return this CSVOptions instance for method chaining.
     */
    public CSVOptions withQuoteChar(char quoteChar) {
        this.quoteChar = quoteChar;
        return this;
    }

    /**
     * Returns the currently configured separator character.
     * 
     * @return the character used to separate fields in the CSV.
     */
    public char getSeparator() {
        return separator;
    }

    /**
     * Returns the currently configured quote character.
     * 
     * @return the character used to quote fields, or
     *         {@code CSVWriter.NO_QUOTE_CHARACTER} if none is set.
     */
    public char getQuoteChar() {
        return quoteChar;
    }

    /**
     * Returns the currently configured escape character.
     * 
     * @return the character used to escape special characters in the CSV.
     */
    public char getEscapeChar() {
        return escapeChar;
    }

    /**
     * Returns the currently configured line ending.
     * 
     * @return the string used to terminate each line in the CSV output.
     */
    public String getLineEnd() {
        return lineEnd;
    }
}
