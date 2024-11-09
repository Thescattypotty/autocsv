package io.github.thescattypotty.autocsv.Service;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import io.github.thescattypotty.autocsv.Annotation.CSVColumn;
import io.github.thescattypotty.autocsv.Exception.CSVConversionException;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A service class responsible for converting collections of Java objects to CSV 
 * and vice versa. It provides methods for serializing objects into CSV format and 
 * deserializing CSV content back into Java objects.
 * <p>
 * This class supports custom options for CSV writing through the {@link CSVOptions} class.
 * </p>
 * 
 * @author Thescattypotty
 */
public class CSVConverter {
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";


    /**
     * Converts a collection of Java objects to a CSV file at the specified file
     * path.
     * <p>
     * The method writes the collection to the CSV file with headers derived from
     * the field
     * names or {@link CSVColumn} annotations. The CSV options (e.g., separator,
     * quote character)
     * can be customized using {@link CSVOptions}.
     * </p>
     * 
     * @param <T>      the type of objects in the collection
     * @param data     the collection of objects to be written to CSV
     * @param filePath the path to the output CSV file
     * @param options  the CSV options used for formatting the output
     * @throws IOException            if an I/O error occurs during writing
     * @throws CSVConversionException if there is an error during conversion
     */
    public <T> void convertToCsv(Collection<T> data, String filePath, CSVOptions options) throws IOException {
        if (data == null || data.isEmpty()) {
            throw new CSVConversionException("No data to convert");
        }

        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath),
                options.getSeparator(),
                options.getQuoteChar(),
                options.getEscapeChar(),
                options.getLineEnd())) {

            T sample = data.iterator().next();
            List<FieldInfo> fields = getFieldsInfo(sample.getClass());

            String[] header = fields.stream()
                    .map(FieldInfo::getColumnName)
                    .toArray(String[]::new);
            writer.writeNext(header);

            for (T item : data) {
                String[] row = new String[fields.size()];
                for (int i = 0; i < fields.size(); i++) {
                    FieldInfo fieldInfo = fields.get(i);
                    Field field = fieldInfo.getField();
                    field.setAccessible(true);

                    try {
                        Object value = field.get(item);
                        row[i] = formatValue(value, fieldInfo);
                    } catch (IllegalAccessException e) {
                        throw new CSVConversionException("Error accessing field: " + field.getName(), e);
                    }
                }
                writer.writeNext(row);
            }
        }
    }


    /**
     * Converts a CSV file into a collection of Java objects of the specified class.
     * <p>
     * The method reads a CSV file, uses the headers to map columns to fields, and
     * creates instances of the specified class by setting field values from the CSV
     * rows.
     * </p>
     * 
     * @param <T>      the type of objects to create from the CSV data
     * @param clazz    the class representing the type of objects to create
     * @param filePath the path to the input CSV file
     * @param options  the CSV options used for parsing the input
     * @return a collection of objects created from the CSV data
     * @throws IOException            if an I/O error occurs during reading
     * @throws CSVConversionException if there is an error during conversion
     */
    public <T> Collection<T> convertFromCSV(Class<T> clazz, String filePath, CSVOptions options) throws IOException {
        if(filePath.isBlank() || !Files.exists(Paths.get(filePath)) ){
            return null;
        }
        List<T> result = new ArrayList<>();
        List<FieldInfo> fields = getFieldsInfo(clazz);
        Map<String, FieldInfo> fieldMap = new HashMap<>();

        for (FieldInfo field : fields) {
            fieldMap.put(field.getColumnName().toLowerCase(), field);
        }

        CSVParserBuilder parserBuilder = new CSVParserBuilder()
                .withSeparator(options.getSeparator())
                .withQuoteChar(options.getQuoteChar())
                .withEscapeChar(options.getEscapeChar());

        try (CSVReader reader = new CSVReaderBuilder(new FileReader(filePath))
                .withCSVParser(parserBuilder.build())
                .build()) {

            String[] header = reader.readNext();
            if (header == null) {
                throw new CSVConversionException("Empty CSV file");
            }

            validateHeader(header, fieldMap);

            Map<Integer, FieldInfo> columnMapping = createColumnMapping(header, fieldMap);
            String[] line;
            int rowNum = 1;

            while ((line = reader.readNext()) != null) {
                rowNum++;
                try {
                    T obj = createInstance(clazz, line, columnMapping, rowNum);
                    result.add(obj);
                } catch (Exception e) {
                    throw new CSVConversionException("Error at row " + rowNum, e);
                }
            }
        } catch (CsvValidationException e1) {
            throw new CSVConversionException("Csv Invalid Exception " + e1);
        }
        return result;
    }


    /**
     * Creates an instance of the specified class, setting its fields based on the
     * values in the CSV row.
     * <p>
     * This method is used during CSV deserialization to populate the fields of an
     * object from the CSV data.
     * </p>
     * 
     * @param <T>           the type of object to create
     * @param clazz         the class to instantiate
     * @param line          the CSV row containing values
     * @param columnMapping the mapping of CSV columns to object fields
     * @param rowNum        the current row number in the CSV (for error reporting)
     * @return an instance of the specified class with fields populated from the CSV
     *         data
     * @throws Exception if there is an error creating the object or setting its
     *                   fields
     */
    private <T> T createInstance(Class<T> clazz, String[] line, Map<Integer, FieldInfo> columnMapping, int rowNum)
            throws Exception {
        Constructor<T> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        T obj = constructor.newInstance();

        for (Map.Entry<Integer, FieldInfo> entry : columnMapping.entrySet()) {
            int index = entry.getKey();
            FieldInfo fieldInfo = entry.getValue();
            Field field = fieldInfo.getField();
            field.setAccessible(true);

            if (index >= line.length) {
                if (fieldInfo.isRequired()) {
                    throw new CSVConversionException(
                            "Missing required field " + fieldInfo.getColumnName() + " at row " + rowNum);
                }
                continue;
            }

            String value = line[index].trim();
            if (value.isEmpty() && fieldInfo.isRequired()) {
                throw new CSVConversionException(
                        "Required field " + fieldInfo.getColumnName() + " is empty at row " + rowNum);
            }

            if (!value.isEmpty()) {
                field.set(obj, parseValue(value, fieldInfo, rowNum));
            }
        }

        return obj;
    }

    /**
     * Parses a CSV field value into the appropriate type for the given field.
     * <p>
     * This method supports conversion of various field types such as
     * {@code String},
     * {@code Integer}, {@code Double}, {@code Date}, and {@code Enum}.
     * </p>
     * 
     * @param value     the string value from the CSV
     * @param fieldInfo the field information including type and format
     * @param rowNum    the current row number in the CSV (for error reporting)
     * @return the parsed value, converted to the appropriate type for the field
     * @throws CSVConversionException if there is an error parsing the value
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object parseValue(String value, FieldInfo fieldInfo, int rowNum) {
        try {
            Class<?> fieldType = fieldInfo.getField().getType();

            if (fieldType == String.class) {
                return value;
            } else if (fieldType == Integer.class || fieldType == int.class) {
                return Integer.parseInt(value);
            } else if (fieldType == Long.class || fieldType == long.class) {
                return Long.parseLong(value);
            } else if (fieldType == Double.class || fieldType == double.class) {
                if (value.contains(",")) {
                    value = value.replace(",", "");
                }
                return Double.parseDouble(value);
            } else if (fieldType == Boolean.class || fieldType == boolean.class) {
                return Boolean.parseBoolean(value);
            } else if (fieldType == Date.class) {
                SimpleDateFormat dateFormat = new SimpleDateFormat(fieldInfo.getDateFormat());
                return dateFormat.parse(value);
            } else if (fieldType.isEnum()) {
                return Enum.valueOf((Class<? extends Enum>) fieldType, value);
            }

            throw new CSVConversionException("Unsupported type: " + fieldType.getName());
        } catch (Exception e) {
            throw new CSVConversionException("Error parsing value '" + value + "' at row " + rowNum +
                    " for column " + fieldInfo.getColumnName(), e);
        }
    }

    /**
     * Formats a field value for writing to CSV.
     * <p>
     * This method handles date formatting, number formatting, and converts
     * objects to their string representations.
     * </p>
     * 
     * @param value     the field value to format
     * @param fieldInfo the field information including format settings
     * @return the formatted string value for the CSV
     */
    private String formatValue(Object value, FieldInfo fieldInfo) {
        if (value == null) {
            return "";
        }

        if (value instanceof Date) {
            SimpleDateFormat dateFormat = new SimpleDateFormat(fieldInfo.getDateFormat());
            return dateFormat.format(value);
        } else if (value instanceof Number && !fieldInfo.getNumberFormat().isEmpty()) {
            DecimalFormat decimalFormat = new DecimalFormat(fieldInfo.getNumberFormat());
            return decimalFormat.format(value);
        }

        return value.toString();
    }

    /**
     * Retrieves metadata about the fields in the given class, including any
     * {@link CSVColumn} annotations.
     * <p>
     * The method gathers information about the fields to determine how they should
     * be serialized or
     * deserialized from the CSV.
     * </p>
     * 
     * @param clazz the class to inspect for fields
     * @return a list of {@link FieldInfo} objects containing metadata about the
     *         class's fields
     */
    private List<FieldInfo> getFieldsInfo(Class<?> clazz) {
        List<FieldInfo> fields = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            CSVColumn annotation = field.getAnnotation(CSVColumn.class);
            if (annotation != null && !annotation.ignore()) {
                fields.add(new FieldInfo(field, annotation));
            } else if (annotation == null) {
                fields.add(new FieldInfo(field, null));
            }
        }
        return fields;
    }

    /**
     * Validates the CSV header row against the expected fields in the class.
     * <p>
     * Ensures that all required fields are present in the header and warns about
     * unknown columns.
     * </p>
     * 
     * @param header   the CSV header row
     * @param fieldMap a map of field names to {@link FieldInfo} objects
     * @throws CSVConversionException if required columns are missing
     */
    private void validateHeader(String[] header, Map<String, FieldInfo> fieldMap) {
        if (header == null || header.length == 0) {
            throw new CSVConversionException("CSV file has no headers");
        }

        Map<String, String> normalizedHeaders = new HashMap<>();
        for (String h : header) {
            normalizedHeaders.put(normalizeColumnName(h), h);
        }

        StringBuilder missingColumns = new StringBuilder();
        for (Map.Entry<String, FieldInfo> entry : fieldMap.entrySet()) {
            FieldInfo fieldInfo = entry.getValue();
            if (fieldInfo.isRequired()) {
                String normalizedName = normalizeColumnName(fieldInfo.getColumnName());
                if (!normalizedHeaders.containsKey(normalizedName)) {
                    if (missingColumns.length() > 0) {
                        missingColumns.append(", ");
                    }
                    missingColumns.append(fieldInfo.getColumnName());
                }
            }
        }

        if (missingColumns.length() > 0) {
            throw new CSVConversionException("Required column(s) missing in CSV: " + missingColumns.toString());
        }

        for (String columnName : header) {
            String normalizedName = normalizeColumnName(columnName);
            boolean found = false;
            for (FieldInfo fieldInfo : fieldMap.values()) {
                if (normalizeColumnName(fieldInfo.getColumnName()).equals(normalizedName)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                System.out.println("Warning: Unknown column in CSV: " + columnName);
            }
        }
    }

    /**
     * Normalizes a column name by removing spaces and non-alphanumeric characters
     * and converting it to lowercase.
     * 
     * @param columnName the column name to normalize
     * @return the normalized column name
     */
    private String normalizeColumnName(String columnName) {
        return columnName.trim()
                .toLowerCase()
                .replaceAll("\\s+", "")
                .replaceAll("[^a-z0-9]", "");
    }

    /**
     * Creates a mapping between CSV columns and object fields based on the header
     * row.
     * 
     * @param header   the CSV header row
     * @param fieldMap a map of field names to {@link FieldInfo} objects
     * @return a mapping of CSV column indices to {@link FieldInfo} objects
     */
    private Map<Integer, FieldInfo> createColumnMapping(String[] header, Map<String, FieldInfo> fieldMap) {
        Map<Integer, FieldInfo> columnMapping = new HashMap<>();

        for (int i = 0; i < header.length; i++) {
            String normalizedHeader = normalizeColumnName(header[i]);

            FieldInfo matchingField = null;
            for (FieldInfo fieldInfo : fieldMap.values()) {
                if (normalizeColumnName(fieldInfo.getColumnName()).equals(normalizedHeader)) {
                    matchingField = fieldInfo;
                    break;
                }
            }

            if (matchingField != null) {
                columnMapping.put(i, matchingField);
            }
        }

        return columnMapping;
    }

    /**
     * A helper class that stores metadata about a field in a Java class.
     * <p>
     * This class captures information from {@link CSVColumn} annotations,
     * including the field's name, whether it is required, and any formatting
     * options.
     * </p>
     */
    private static class FieldInfo {
        private final Field field;
        private final String columnName;
        private final boolean required;
        private final String dateFormat;
        private final String numberFormat;

        /**
         * Constructs a new FieldInfo object for the given field.
         * 
         * @param field      the field in the class
         * @param annotation the {@link CSVColumn} annotation on the field, or null if
         *                   not annotated
         */
        public FieldInfo(Field field, CSVColumn annotation) {
            this.field = field;
            this.columnName = (annotation != null && !annotation.name().isEmpty())
                    ? annotation.name()
                    : field.getName();
            this.required = annotation != null && annotation.required();
            this.dateFormat = annotation != null ? annotation.dateFormat() : DEFAULT_DATE_FORMAT;
            this.numberFormat = annotation != null ? annotation.numberFormat() : "";
        }

        /**
         * Gets the field represented by this FieldInfo.
         * 
         * @return the field
         */
        public Field getField() {
            return field;
        }

        /**
         * Gets the CSV column name corresponding to this field.
         * 
         * @return the CSV column name
         */
        public String getColumnName() {
            return columnName;
        }

        /**
         * Checks if the field is marked as required.
         * 
         * @return true if the field is required, false otherwise
         */
        public boolean isRequired() {
            return required;
        }

        /**
         * Gets the date format used for this field, if it is a date.
         * 
         * @return the date format string
         */
        public String getDateFormat() {
            return dateFormat;
        }

        /**
         * Gets the number format used for this field, if applicable.
         * 
         * @return the number format string
         */
        public String getNumberFormat() {
            return numberFormat;
        }
    }
}