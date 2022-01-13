package ca.bc.gov.educ.api.dataconversion.model;

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
    private List<String> optionalProgramCodes = new ArrayList<>();

    // career program codes
    private List<String> careerProgramCodes = new ArrayList<>();

    // courses

    // assessments
}
