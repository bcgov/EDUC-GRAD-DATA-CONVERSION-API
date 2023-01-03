package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StudentCommonDTO extends StudentDemographicDTO {
    // grad status
    private String program;
    private String schoolOfRecord;
    private String schoolAtGrad;
    private String studentGrade;
    private String studentStatus;
    private String gradDate; // TODO(jsung): populate with programCompletionDate

    // citizenship
    private String citizenship;

    // all program codes
    private List<String> programCodes = new ArrayList<>();

    // courses
    private List<StudentCourse> courses = new ArrayList<>();

    // assessments
    private List<StudentAssessment> assessments = new ArrayList<>();
}
