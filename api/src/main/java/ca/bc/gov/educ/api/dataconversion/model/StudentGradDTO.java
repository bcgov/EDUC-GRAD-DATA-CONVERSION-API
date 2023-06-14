package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Data
public class StudentGradDTO extends  StudentCommonDTO {
    private UUID studentID;

    // new values for update
    private boolean addDualDogwood = false;
    private boolean deleteDualDogwood = false;
    private boolean deleteFrenchImmersion = false;

    private String newProgram;
    private String newSchoolOfRecord;
    private String newSchoolAtGrad;
    private String newStudentGrade;
    private String newStudentStatus;
    private String newAdultStartDate;

    // new flags
    private String newRecalculateGradStatus;
    private String newRecalculateProjectedGrad;

    private List<String> addedProgramCodes = new ArrayList<>();
    private List<String> removedProgramCodes = new ArrayList<>();
}
