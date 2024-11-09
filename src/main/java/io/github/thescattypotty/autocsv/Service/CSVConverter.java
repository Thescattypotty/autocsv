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

public class CSVConverter {
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";

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

    private String normalizeColumnName(String columnName) {
        return columnName.trim()
                .toLowerCase()
                .replaceAll("\\s+", "")
                .replaceAll("[^a-z0-9]", "");
    }

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

    private static class FieldInfo {
        private final Field field;
        private final String columnName;
        private final boolean required;
        private final String dateFormat;
        private final String numberFormat;

        public FieldInfo(Field field, CSVColumn annotation) {
            this.field = field;
            this.columnName = (annotation != null && !annotation.name().isEmpty())
                    ? annotation.name()
                    : field.getName();
            this.required = annotation != null && annotation.required();
            this.dateFormat = annotation != null ? annotation.dateFormat() : DEFAULT_DATE_FORMAT;
            this.numberFormat = annotation != null ? annotation.numberFormat() : "";
        }

        public Field getField() {
            return field;
        }

        public String getColumnName() {
            return columnName;
        }

        public boolean isRequired() {
            return required;
        }

        public String getDateFormat() {
            return dateFormat;
        }

        public String getNumberFormat() {
            return numberFormat;
        }
    }
}