package io.github.thescarrypotty.autocsv.Entity;

import java.util.Date;

import io.github.thescattypotty.autocsv.Annotation.CSVColumn;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Person {
    @CSVColumn(name = "First Name", required = true)
    private String firstName;

    @CSVColumn(name = "Last Name", required = true)
    private String lastName;

    @CSVColumn(name = "Birth Date", dateFormat = "dd/MM/yyyy")
    private Date birthDate;

    @CSVColumn(name = "Salary", numberFormat = "#,##0.00")
    private Double salary;

    @CSVColumn(ignore = true)
    private String internalNote;

    @Override
    public String toString() {
        return "Person [firstName=" + firstName + ", lastName=" + lastName + ", birthDate=" + birthDate + ", salary="
                + salary + ", internalNote=" + internalNote + "]";
    }
}
