package io.github.thescarrypotty.autocsv;

import junit.framework.TestCase;
import io.github.thescarrypotty.autocsv.Entity.Person;
import io.github.thescattypotty.autocsv.Exception.CSVConversionException;
import io.github.thescattypotty.autocsv.Service.CSVConverter;
import io.github.thescattypotty.autocsv.Service.CSVOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class AppTest extends TestCase {

    private CSVConverter csvConverter;
    private List<Person> testPeople;
    private String csvFilePath;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        csvConverter = new CSVConverter();
        csvFilePath = "test_people.csv";

        testPeople = new ArrayList<>();
        testPeople.add(new Person("John", "Doe", new Date(), 3500.00, "Internal Note 1"));
        testPeople.add(new Person("Jane", "Doe", new Date(), 4200.50, "Internal Note 2"));
        testPeople.add(new Person("Alice", "Smith", new Date(), 4700.75, "Internal Note 3"));
        testPeople.add(new Person("Bob", "Johnson", new Date(), 5100.25, "Internal Note 4"));
    }

    @Override
    protected void tearDown() throws Exception {
        File file = new File(csvFilePath);
        if (file.exists()) {
            file.delete();
        }
        super.tearDown();
    }

    public void testConvertToCsv() {
        CSVOptions options = CSVOptions.defaultOptions()
                .withSeparator(',')
                .withQuoteChar('"');

        try {
            csvConverter.convertToCsv(testPeople, csvFilePath, options);

            File file = new File(csvFilePath);
            assertTrue("CSV file should be created", file.exists());

            Collection<Person> peopleFromCsv = csvConverter.convertFromCSV(Person.class, csvFilePath, options);
            assertNotNull("Converted list from CSV should not be null", peopleFromCsv);
            assertEquals("Number of people in the CSV file should match", testPeople.size(), peopleFromCsv.size());

        } catch (IOException e) {
            fail("IOException occurred during CSV conversion: " + e.getMessage());
        }
    }

    public void testConvertFromCSV() {
        CSVOptions options = CSVOptions.defaultOptions()
                .withSeparator(',')
                .withQuoteChar('"');

        try {
            csvConverter.convertToCsv(testPeople, csvFilePath, options);

            Collection<Person> peopleFromCsv = csvConverter.convertFromCSV(Person.class, csvFilePath, options);
            assertNotNull("Converted list from CSV should not be null", peopleFromCsv);
            assertEquals("Number of people in the CSV file should match", testPeople.size(), peopleFromCsv.size());

            Person person = ((List<Person>) peopleFromCsv).get(0);
            assertNotNull("Person should not be null", person);
            assertEquals("First Name should match", "John", person.getFirstName());
            assertEquals("Last Name should match", "Doe", person.getLastName());
            assertNotNull("Birth Date should not be null", person.getBirthDate());
            assertTrue("Salary should be greater than 0", person.getSalary() > 0);

        } catch (IOException e) {
            fail("IOException occurred during CSV conversion: " + e.getMessage());
        } catch (CSVConversionException e) {
            fail("CSVConversionException occurred during conversion: " + e.getMessage());
        }
    }

    public void testCSVConversionWithInvalidFile() {
        CSVOptions options = CSVOptions.defaultOptions()
                .withSeparator(',')
                .withQuoteChar('"');

        try {
            csvConverter.convertToCsv(testPeople, csvFilePath, options);

            Collection<Person> peopleFromInvalidFile = csvConverter.convertFromCSV(Person.class, "invalid_file.csv", options);
            assertNull("Should return null for non-existing file", peopleFromInvalidFile);
        } catch (IOException e) {
            fail("IOException occurred during CSV conversion: " + e.getMessage());
        }
    }
}
