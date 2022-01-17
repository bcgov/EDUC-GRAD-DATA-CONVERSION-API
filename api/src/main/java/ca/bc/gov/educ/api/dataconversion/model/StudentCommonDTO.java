package ca.bc.gov.educ.api.dataconversion.model;

import ca.bc.gov.educ.api.dataconversion.entity.assessment.StudentAssessmentEntity;
import ca.bc.gov.educ.api.dataconversion.entity.course.StudentCourseEntity;
import ca.bc.gov.educ.api.dataconversion.entity.student.StudentCareerProgramEntity;
import ca.bc.gov.educ.api.dataconversion.entity.student.StudentOptionalProgramEntity;
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

    // optional program codes
    private List<StudentOptionalProgramEntity> optionalPrograms = new ArrayList<>();

    // career program codes
    private List<StudentCareerProgramEntity> careerPrograms = new ArrayList<>();

    private List<String> programCodes = new ArrayList<>();

    // courses
    private List<StudentCourseEntity> courses = new ArrayList<>();

    // assessments
    private List<StudentAssessmentEntity> assessments = new ArrayList<>();
}
