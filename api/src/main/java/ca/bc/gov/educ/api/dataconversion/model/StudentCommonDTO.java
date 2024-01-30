package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Data
public class StudentCommonDTO extends StudentDemographicDTO {
    // grad status
    private String program;
    private String gradDate;
    private String schoolOfRecord;
    private String schoolAtGrad;
    private String studentGrade;
    private String studentStatus;

    // citizenship
    private String citizenship;

    // adultStartDate
    private String adultStartDate;

    // all program codes
    private List<String> programCodes = new ArrayList<>();

    // courses
    private List<StudentCourse> courses = new ArrayList<>();

    // assessments
    private List<StudentAssessment> assessments = new ArrayList<>();

    public boolean isGraduated() {
        return StringUtils.isNotBlank(gradDate);
    }

    public boolean isSCCP() {
        return "SCCP".equalsIgnoreCase(program);
    }

    public boolean isArchived() {
        return "ARC".equalsIgnoreCase(studentStatus);
    }
}
