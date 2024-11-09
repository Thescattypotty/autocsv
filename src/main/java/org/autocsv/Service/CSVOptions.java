package org.autocsv.Service;

import com.opencsv.CSVWriter;

public class CSVOptions {
    private char separator = CSVWriter.DEFAULT_SEPARATOR;
    private char quoteChar = CSVWriter.NO_QUOTE_CHARACTER;
    private char escapeChar = CSVWriter.NO_ESCAPE_CHARACTER;
    private String lineEnd = CSVWriter.DEFAULT_LINE_END;

    public static CSVOptions defaultOptions() {
        return new CSVOptions();
    }

    public CSVOptions withSeparator(char separator) {
        this.separator = separator;
        return this;
    }

    public CSVOptions withQuoteChar(char quoteChar) {
        this.quoteChar = quoteChar;
        return this;
    }

    public char getSeparator() {
        return separator;
    }

    public char getQuoteChar() {
        return quoteChar;
    }

    public char getEscapeChar() {
        return escapeChar;
    }

    public String getLineEnd() {
        return lineEnd;
    }
}
