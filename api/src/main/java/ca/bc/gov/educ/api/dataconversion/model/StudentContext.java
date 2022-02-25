package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Data;

@Data
public class StudentContext {
    // Trax Student
    private StudentTraxDTO traxStudent;

    // Grad Student
    private StudentGradDTO gradStudent;
}
