package ca.bc.gov.educ.api.dataconversion.service.student;

import ca.bc.gov.educ.api.dataconversion.constant.ConversionResultType;
import ca.bc.gov.educ.api.dataconversion.model.ConvGradStudent;
import ca.bc.gov.educ.api.dataconversion.model.ConversionAlert;
import ca.bc.gov.educ.api.dataconversion.model.ConversionStudentSummaryDTO;
import org.apache.commons.lang3.StringUtils;

public class StudentBaseService {

    public static final String STUDENT_STATUS_CURRENT = "CUR";
    public static final String STUDENT_STATUS_ARCHIVED = "ARC";
    public static final String STUDENT_STATUS_DECEASED = "DEC";
    public static final String STUDENT_STATUS_MERGED = "MER";
    public static final String STUDENT_STATUS_TERMINATED = "TER";

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
        } else if (StringUtils.equalsIgnoreCase(traxStudentStatus, "T") &&
                (StringUtils.equalsIgnoreCase(traxArchiveFlag, "A") || StringUtils.equalsIgnoreCase(traxArchiveFlag, "I")) ) {
            return STUDENT_STATUS_TERMINATED;
        }
        return null;
    }

    protected boolean determineProgram(ConvGradStudent student, ConversionStudentSummaryDTO summary) {
        String gradProgram = student.isGraduated() ?
            getGradProgramForGraduatedStudent(student.getGraduationRequirementYear(), student.getSchoolOfRecord(), student.getFrenchCert(), student.getStudentGrade())
            : getGradProgram(student.getGraduationRequirementYear(), student.getSchoolOfRecord(), student.getFrenchDogwood());
        if (StringUtils.isNotBlank(gradProgram)) {
            student.setProgram(gradProgram);
            updateProgramCountsInSummary(summary, gradProgram, student.isGraduated());
            return true;
        } else {
            // error
            handleException(student, summary, student.getPen(), ConversionResultType.FAILURE, "Program is not found for grad_reqt_year " + student.getGraduationRequirementYear() + " / grade " + student.getStudentGrade());
            return false;
        }
    }

    protected String getGradProgram(String graduationRequirementYear, String schoolOfRecord, String frenchDogwood) {
        String gradProgram = null;
        switch(graduationRequirementYear) {
            case "2018":
                if (schoolOfRecord.startsWith("093")) {
                    gradProgram = "2018-PF";
                } else {
                    gradProgram = "2018-EN";
                }
                break;
            case "2004":
                if (schoolOfRecord.startsWith("093")) {
                    gradProgram = "2004-PF";
                } else {
                    gradProgram = "2004-EN";
                }
                break;
            case "1996":
                if (schoolOfRecord.startsWith("093")) {
                    gradProgram = "1996-PF";
                } else {
                    gradProgram = "1996-EN";
                }
                break;
            case "1986":
                if ("Y".equalsIgnoreCase(frenchDogwood)) {
                    gradProgram = "1986-PF";
                } else {
                    gradProgram = "1986-EN";
                }
                break;
            case "1950":
                gradProgram = "1950";
                break;
            case "SCCP":
                gradProgram = "SCCP";
                break;
            default:
                return null;
        }
        return gradProgram;
    }

    protected String getGradProgramForGraduatedStudent(String graduationRequirementYear, String schoolOfRecord, String frenchCert, String studentGrade) {
        String gradProgram = null;
        switch(graduationRequirementYear) {
            case "2018":
                if (schoolOfRecord.startsWith("093") &&
                    (StringUtils.equalsIgnoreCase(frenchCert, "F") || StringUtils.equalsIgnoreCase(frenchCert, "S")) ) {
                    gradProgram = "2018-PF";
                } else {
                    gradProgram = "2018-EN";
                }
                break;
            case "2004":
                if (schoolOfRecord.startsWith("093") &&
                    (StringUtils.equalsIgnoreCase(frenchCert, "F") || StringUtils.equalsIgnoreCase(frenchCert, "S")) ) {
                    gradProgram = "2004-PF";
                } else {
                    gradProgram = "2004-EN";
                }
                break;
            case "1996":
            case "1995":
                if (schoolOfRecord.startsWith("093") &&
                    (StringUtils.equalsIgnoreCase(frenchCert, "F") || StringUtils.equalsIgnoreCase(frenchCert, "S")) ) {
                    gradProgram = "1996-PF";
                } else {
                    gradProgram = "1996-EN";
                }
                break;
            case "1986":
                gradProgram = "1986-EN";
                break;
            case "1950":
                if (StringUtils.equalsIgnoreCase(studentGrade, "AD")) {
                    gradProgram = "1950";
                }
                break;
            case "SCCP":
                gradProgram = "SCCP";
                break;
            default:
                return null;
        }
        return gradProgram;
    }

    private void updateProgramCountsInSummary(ConversionStudentSummaryDTO summary, String programCode, boolean isGraduated) {
        if (summary != null) {
            summary.increment(programCode, isGraduated);
        }
    }

}
