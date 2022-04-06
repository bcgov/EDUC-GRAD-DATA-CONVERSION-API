package ca.bc.gov.educ.api.dataconversion.model;

import ca.bc.gov.educ.api.dataconversion.entity.course.StudentCourseEntity;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StudentCommonDTO {
    // grad status
    private String program;
    private String schoolOfRecord;
    private String schoolAtGrad;
    private String studentGrade;
    private String studentStatus;

    // all program codes
    private List<String> programCodes = new ArrayList<>();

    // courses
    private List<StudentCourseEntity> courses = new ArrayList<>();

    // assessments
    private List<StudentAssessment> assessments = new ArrayList<>();
}
