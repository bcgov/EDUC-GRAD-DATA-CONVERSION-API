package ca.bc.gov.educ.api.dataconversion.model.tsw.report;

import java.io.Serializable;

public class AssessmentResult implements Serializable {

    private static final long serialVersionUID = 2L;

    private String assessmentName;
    private String assessmentCode;
    private String proficiencyScore;
    private String sessionDate;
    private String gradReqMet;
    private String specialCase;
    private String exceededWriteFlag;
    private boolean projected;

    public String getAssessmentName() {
        return assessmentName;
    }

    public void setAssessmentName(String assessmentName) {
        this.assessmentName = assessmentName;
    }

    public String getAssessmentCode() {
        return assessmentCode;
    }

    public void setAssessmentCode(String assessmentCode) {
        this.assessmentCode = assessmentCode;
    }

    public String getProficiencyScore() {
        return proficiencyScore;
    }

    public void setProficiencyScore(String proficiencyScore) {
        this.proficiencyScore = proficiencyScore;
    }

    public String getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(String sessionDate) {
        this.sessionDate = sessionDate;
    }

    public String getGradReqMet() {
        return gradReqMet;
    }

    public void setGradReqMet(String gradReqMet) {
        this.gradReqMet = gradReqMet;
    }

    public String getSpecialCase() {
        return specialCase;
    }

    public void setSpecialCase(String specialCase) {
        this.specialCase = specialCase;
    }

    public String getExceededWriteFlag() {
        return exceededWriteFlag;
    }

    public void setExceededWriteFlag(String exceededWriteFlag) {
        this.exceededWriteFlag = exceededWriteFlag;
    }

    public boolean getProjected() {
        return projected;
    }

    public void setProjected(boolean projected) {
        this.projected = projected;
    }
}
