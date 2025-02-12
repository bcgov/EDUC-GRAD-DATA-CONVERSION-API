package ca.bc.gov.educ.api.dataconversion.service.student;

import ca.bc.gov.educ.api.dataconversion.model.StudentGradDTO;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiUtils;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.util.Date;
import java.util.UUID;

public abstract class StudentGraduationUpdateBaseService extends StudentBaseService {

    protected StudentGraduationUpdateBaseService(RestUtils restUtils) {
        super(restUtils);
    }

    protected abstract boolean hasAnyFrenchImmersionCourse(String gradProgramCode, String pen, String accessToken);

    protected void handleProgramChange(String newGradProgram, StudentGradDTO currentStudent, String pen, String accessToken) {
        boolean addDualDogwood = false;
        boolean addFrenchImmersion = false;

        if (!currentStudent.isSCCP() && currentStudent.isGraduated()) {
            currentStudent.setNewProgram(null);
            return;
        }

        if (newGradProgram.endsWith("-PF")) {
            // from EN to PF
            // from PF to PF
            // from 1950 to PF
            // from SCCP to PF
            addDualDogwood = true;
        } else if (newGradProgram.endsWith("-EN") && (currentStudent.getProgram().endsWith("-PF") || hasAnyFrenchImmersionCourse(newGradProgram, pen, accessToken))) {
            // from PF to EN - allowed for SD93/Yukon PF schools
            // from EN to EN
            // from 1950 to EN
            // from SCCP to EN
            addFrenchImmersion = true;
        }

        currentStudent.setAddDualDogwood(addDualDogwood);
        currentStudent.setAddFrenchImmersion(addFrenchImmersion);

        currentStudent.setNewProgram(newGradProgram);
    }

    protected void handleAdultStartDate(StudentGradDTO currentStudent) {
        if (StringUtils.equalsIgnoreCase(currentStudent.getNewProgram(), "1950") && StringUtils.isBlank(currentStudent.getAdultStartDate())) {
            Date dob = EducGradDataConversionApiUtils.parseDate(currentStudent.getBirthday());
            Date adultStartDate = DateUtils.addYears(dob, 18);
            currentStudent.setNewAdultStartDate(EducGradDataConversionApiUtils.formatDate(adultStartDate)); // yyyy-MM-dd
        }
    }

    protected boolean processSchoolOfRecordId(StudentGradDTO currentStudent, UUID value) {
        boolean isChanged = false;
        switch(currentStudent.getNewStudentStatus()) {
            case STUDENT_STATUS_CURRENT -> {
                // UpdData
                currentStudent.setNewSchoolOfRecordId(value);
                // Transcript
                currentStudent.setNewRecalculateGradStatus("Y");
                // TVR
                currentStudent.setNewRecalculateProjectedGrad("Y");
                isChanged = true;
            }
            case STUDENT_STATUS_ARCHIVED -> {
                if (!currentStudent.isGraduated()) {
                    // UpdData
                    currentStudent.setNewSchoolOfRecordId(value);
                    // Transcript
                    currentStudent.setNewRecalculateGradStatus("Y");
                    isChanged = true;
                }
            }
            case STUDENT_STATUS_TERMINATED -> {
                // UpdData
                currentStudent.setNewSchoolOfRecordId(value);
                // Transcript
                currentStudent.setNewRecalculateGradStatus("Y");
                isChanged = true;
            }
            default -> { // MER or DEC
                // UpdData
                currentStudent.setNewSchoolOfRecordId(value);
                // Do not set flags to Y
                isChanged = true;
            }
        }
        return isChanged;
    }

    protected boolean processStudentGrade(StudentGradDTO currentStudent, String value) {
        boolean isChanged = false;
        switch(currentStudent.getNewStudentStatus()) {
            case STUDENT_STATUS_CURRENT -> {
                // UpdData
                currentStudent.setNewStudentGrade(value);
                // TVR
                currentStudent.setNewRecalculateProjectedGrad("Y");
                if (!currentStudent.isGraduated()) {
                    // Transcript
                    currentStudent.setNewRecalculateGradStatus("Y");
                }
                isChanged = true;
            }
            case STUDENT_STATUS_ARCHIVED -> {
                if (!currentStudent.isGraduated()) {
                    // UpdData
                    currentStudent.setNewStudentGrade(value);
                    // Transcript
                    currentStudent.setNewRecalculateGradStatus("Y");
                    isChanged = true;
                }
            }
            case STUDENT_STATUS_TERMINATED -> {
                // UpdData
                currentStudent.setNewStudentGrade(value);
                // Transcript
                currentStudent.setNewRecalculateGradStatus("Y");
                isChanged = true;
            }
            default -> { // MER or DEC
                // UpdData
                currentStudent.setNewStudentGrade(value);
                // Do not set flags to Y
                isChanged = true;
            }
        }
        return isChanged;
    }

    protected boolean processGraduationProgram(StudentGradDTO currentStudent, String pen, String gradProgram, String accessToken) {
        boolean isChanged = false;
        switch(currentStudent.getNewStudentStatus()) {
            case STUDENT_STATUS_CURRENT -> {
                if (!currentStudent.isGraduated() || currentStudent.isSCCP()) {
                    // UpdData
                    handleProgramChange(gradProgram, currentStudent, pen, accessToken);
                    handleAdultStartDate(currentStudent);
                    // Transcript
                    currentStudent.setNewRecalculateGradStatus("Y");
                    // TVR
                    currentStudent.setNewRecalculateProjectedGrad("Y");
                    isChanged = true;
                }
            }
            case STUDENT_STATUS_ARCHIVED, STUDENT_STATUS_TERMINATED -> {
                if (!currentStudent.isGraduated() || currentStudent.isSCCP()) {
                    // UpdData
                    handleProgramChange(gradProgram, currentStudent, pen, accessToken);
                    handleAdultStartDate(currentStudent);
                    // Transcript
                    currentStudent.setNewRecalculateGradStatus("Y");
                    isChanged = true;
                }
            }
            default -> { // MER or DEC
                if (!currentStudent.isGraduated() || currentStudent.isSCCP()) {
                    // UpdData
                    handleProgramChange(gradProgram, currentStudent, pen, accessToken);
                    handleAdultStartDate(currentStudent);
                    // Do not set flags to Y
                    isChanged = true;
                }
            }

        }
        return isChanged;
    }

    protected boolean processSlpDate(StudentGradDTO currentStudent, String value) {
        boolean isChanged = false;
        switch(currentStudent.getNewStudentStatus()) {
            case STUDENT_STATUS_CURRENT -> {
                if (!currentStudent.isGraduated()) {
                    // UpdData
                    currentStudent.setNewGradDate(value);
                    // Transcript
                    currentStudent.setNewRecalculateGradStatus("Y");
                    // TVR
                    currentStudent.setNewRecalculateProjectedGrad("Y");
                    isChanged = true;
                }
            }
            case STUDENT_STATUS_ARCHIVED, STUDENT_STATUS_TERMINATED -> {
                if (!currentStudent.isGraduated()) {
                    // UpdData
                    currentStudent.setNewGradDate(value);
                    // Transcript
                    currentStudent.setNewRecalculateGradStatus("Y");
                    isChanged = true;
                }
            }
            default -> { // MER or DEC
                if (!currentStudent.isGraduated()) {
                    // UpdData
                    currentStudent.setNewGradDate(value);
                    // Do not set flags to Y
                    isChanged = true;
                }
            }

        }
        return isChanged;
    }

    protected boolean processCitizenship(StudentGradDTO currentStudent, String value) {
        boolean isChanged = false;
        switch(currentStudent.getNewStudentStatus()) {
            case STUDENT_STATUS_CURRENT -> {
                // UpdData
                currentStudent.setNewCitizenship(value);
                // TVR
                currentStudent.setNewRecalculateProjectedGrad("Y");
                if (!currentStudent.isGraduated()) {
                    // Transcript
                    currentStudent.setNewRecalculateGradStatus("Y");
                }
                isChanged = true;
            }
            case STUDENT_STATUS_ARCHIVED -> {
                if (!currentStudent.isGraduated()) {
                    // UpdData
                    currentStudent.setNewCitizenship(value);
                    // Transcript
                    currentStudent.setNewRecalculateGradStatus("Y");
                    isChanged = true;
                }
            }
            case STUDENT_STATUS_TERMINATED -> {
                // UpdData
                currentStudent.setNewCitizenship(value);
                // Transcript
                currentStudent.setNewRecalculateGradStatus("Y");
                isChanged = true;
            }
            default -> { // MER or DEC
                // UpdData
                currentStudent.setNewCitizenship(value);
                // Do not set flags to Y
                isChanged = true;
            }

        }
        return isChanged;
    }

    protected void processStudentStatus(StudentGradDTO currentStudent, String value) {
        // UpdData
        currentStudent.setNewStudentStatus(value);
        switch(value) {
            case STUDENT_STATUS_ARCHIVED, STUDENT_STATUS_MERGED -> {
                // TVR
                currentStudent.setNewRecalculateProjectedGrad(NULL_VALUE);
            }
            case STUDENT_STATUS_CURRENT -> {
                // Transcript
                currentStudent.setNewRecalculateGradStatus("Y");
                // TVR
                currentStudent.setNewRecalculateProjectedGrad("Y");
            }
            case STUDENT_STATUS_TERMINATED -> {
                // Transcript
                currentStudent.setNewRecalculateGradStatus("Y");
                // TVR
                currentStudent.setNewRecalculateProjectedGrad(NULL_VALUE);
            }
            default -> { // DEC
            }
        }
    }

}
