package ca.bc.gov.educ.api.dataconversion.service.student;

import ca.bc.gov.educ.api.dataconversion.constant.ConversionResultType;
import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import ca.bc.gov.educ.api.dataconversion.model.ConversionAlert;
import ca.bc.gov.educ.api.dataconversion.model.ConversionStudentSummaryDTO;
import ca.bc.gov.educ.api.dataconversion.model.StudentGradDTO;
import ca.bc.gov.educ.api.dataconversion.model.institute.School;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public abstract class StudentBaseService {

    // NULL String => Nullify (set to NULL)
    public static final String NULL_VALUE = "NULL";

    // Student Status
    public static final String STUDENT_STATUS_CURRENT = "CUR";
    public static final String STUDENT_STATUS_ARCHIVED = "ARC";
    public static final String STUDENT_STATUS_DECEASED = "DEC";
    public static final String STUDENT_STATUS_MERGED = "MER";
    public static final String STUDENT_STATUS_TERMINATED = "TER";

    // Optional Program Codes
    private static final List<String> OPTIONAL_PROGRAM_CODES = Arrays.asList("AD", "BC", "BD");
    private static final List<String> OPTIONAL_PROGRAM_CODES_FOR_RECREATION = Arrays.asList("AD", "BC", "BD", "CP");
    private static final List<String> OPTIONAL_PROGRAM_CODES_FOR_SCCP_RECREATION = Arrays.asList("FR", "CP");

    private final RestUtils restUtils;

    protected StudentBaseService(RestUtils restUtils) {
        this.restUtils = restUtils;
    }

    protected void handleException(ConvGradStudent convGradStudent, ConversionStudentSummaryDTO summary, String pen, ConversionResultType type, String reason) {
        ConversionAlert error = new ConversionAlert();
        error.setItem(pen);
        error.setReason(reason);
        summary.getErrors().add(error);
        if (convGradStudent != null) {
            convGradStudent.setResult(type);
        }
    }

    protected String getGradStudentStatus(String traxStudentStatus, String traxArchiveFlag) {
        if (StringUtils.equalsIgnoreCase(traxStudentStatus, "A") && StringUtils.equalsIgnoreCase(traxArchiveFlag, "A")) {
            return STUDENT_STATUS_CURRENT;
        } else if (StringUtils.equalsIgnoreCase(traxStudentStatus, "A") && StringUtils.equalsIgnoreCase(traxArchiveFlag, "I")) {
            return STUDENT_STATUS_ARCHIVED;
        } else if (StringUtils.equalsIgnoreCase(traxStudentStatus, "D")) {
            return STUDENT_STATUS_DECEASED;
        } else if (StringUtils.equalsIgnoreCase(traxStudentStatus, "M")) {
            return STUDENT_STATUS_MERGED;
        } else if (StringUtils.equalsIgnoreCase(traxStudentStatus, "T") && StringUtils.equalsIgnoreCase(traxArchiveFlag, "A")) {
            return STUDENT_STATUS_TERMINATED;
        } else if (StringUtils.equalsIgnoreCase(traxStudentStatus, "T") && StringUtils.equalsIgnoreCase(traxArchiveFlag, "I")) {
            return STUDENT_STATUS_ARCHIVED;
        }
        return null;
    }

    protected boolean determineProgram(ConvGradStudent student, ConversionStudentSummaryDTO summary) {
        String gradProgram = getGradProgram(student.getGraduationRequirementYear(), student.getSchoolOfRecordId(), student.getFrenchDogwood());
        if (StringUtils.isNotBlank(gradProgram)) {
            student.setProgram(gradProgram);
            updateProgramCountsInSummary(summary, gradProgram, false);
            return true;
        } else {
            // error
            handleException(student, summary, student.getPen(), ConversionResultType.FAILURE, "Program is not found for grad_reqt_year " + student.getGraduationRequirementYear() + " / grade " + student.getStudentGrade());
            return false;
        }
    }

    protected String getGradProgram(String graduationRequirementYear, UUID schoolOfRecordId, String frenchDogwood) {
        String gradProgram = null;
        switch (graduationRequirementYear) {
            case "2023" -> {
                if (isSchoolForProgramFrancophone(schoolOfRecordId)) {
                    gradProgram = "2023-PF";
                } else {
                    gradProgram = "2023-EN";
                }
            }
            case "2018" -> {
                if (isSchoolForProgramFrancophone(schoolOfRecordId)) {
                    gradProgram = "2018-PF";
                } else {
                    gradProgram = "2018-EN";
                }
            }
            case "2004" -> {
                if (isSchoolForProgramFrancophone(schoolOfRecordId)) {
                    gradProgram = "2004-PF";
                } else {
                    gradProgram = "2004-EN";
                }
            }
            case "1996", "1995" -> {
                if (isSchoolForProgramFrancophone(schoolOfRecordId)) {
                    gradProgram = "1996-PF";
                } else {
                    gradProgram = "1996-EN";
                }
            }
            case "1986" -> {
                if ("Y".equalsIgnoreCase(frenchDogwood)) {
                    gradProgram = "1986-PF";
                } else {
                    gradProgram = "1986-EN";
                }
            }
            case "1950" -> gradProgram = "1950";
            case "SCCP" -> gradProgram = "SCCP";
        }
        return gradProgram;
    }

    protected void populateNewBatchFlags(StudentGradDTO currentStudent) {
        switch(currentStudent.getStudentStatus()) {
            case STUDENT_STATUS_CURRENT -> {
                // Transcript
                currentStudent.setNewRecalculateGradStatus("Y");
                // TVR
                currentStudent.setNewRecalculateProjectedGrad("Y");
            }
            case STUDENT_STATUS_ARCHIVED, STUDENT_STATUS_TERMINATED ->
                // Transcript
                currentStudent.setNewRecalculateGradStatus("Y");
            default -> {
                // do not set flags to Y
                currentStudent.setNewRecalculateGradStatus(null);
                currentStudent.setNewRecalculateProjectedGrad(null);
            }
        }
    }

    private void updateProgramCountsInSummary(ConversionStudentSummaryDTO summary, String programCode, boolean isGraduated) {
        if (summary != null) {
            summary.increment(programCode, isGraduated);
        }
    }

    public boolean isOptionalProgramCode(String code) {
        return OPTIONAL_PROGRAM_CODES.contains(code);
    }

    public boolean isOptionalProgramRecreationRequired(String code, String program) {
        return "SCCP".equalsIgnoreCase(program)? OPTIONAL_PROGRAM_CODES_FOR_SCCP_RECREATION.contains(code) : OPTIONAL_PROGRAM_CODES_FOR_RECREATION.contains(code);
    }

    protected boolean isSchoolForProgramFrancophone(UUID schoolOfRecordId) {
        String accessToken = restUtils.fetchAccessToken();
        School school = restUtils.getSchool(schoolOfRecordId, accessToken);
        return school != null && "CSF".equalsIgnoreCase(school.getSchoolReportingRequirementCode());
    }

}
