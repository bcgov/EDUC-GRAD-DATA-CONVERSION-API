package ca.bc.gov.educ.api.dataconversion.model;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Data
public class StudentGradDTO extends  StudentCommonDTO {
    private UUID studentID;

    // new values for update
    private boolean addDualDogwood = false;
    private boolean addFrenchImmersion = false;

    private String newProgram;
    private String newGradDate;
    private String newSchoolOfRecord;
    private UUID newSchoolOfRecordId;
    private String newStudentGrade;
    private String newStudentStatus;
    private String newCitizenship;
    private String newAdultStartDate;

    // new flags
    private String newRecalculateGradStatus;
    private String newRecalculateProjectedGrad;

    private List<String> addedProgramCodes = new ArrayList<>();
    private List<String> removedProgramCodes = new ArrayList<>();

    public String getUpToDateGradProgram() {
        return StringUtils.isNotBlank(newProgram)? newProgram : this.getProgram();
    }

    public String getUpToDateSchoolOfRecord() {
        return StringUtils.isNotBlank(newSchoolOfRecord)? newSchoolOfRecord : this.getSchoolOfRecord();
    }

    @Override
    public boolean isArchived() {
        if (StringUtils.isNotBlank(newStudentStatus) && "ARC".equalsIgnoreCase(newStudentStatus)) { // Student Status from TRAX
            return true;
        } else {
            return super.isArchived(); // Student Status from GRAD
        }
    }

}
