package ca.bc.gov.educ.api.dataconversion.model.tsw.report;

import java.io.Serializable;

public class Exam implements Serializable {

    private static final long serialVersionUID = 2L;

    private String courseCode;
    private String courseName;
    private String courseLevel;
    private String sessionDate;
    private String gradReqMet;
    private String completedCoursePercentage;
    private String completedCourseLetterGrade;
    private String bestSchoolPercent;
    private String bestExamPercent;
    private String interimPercent;
    private String equivOrChallenge;
    private String metLitNumRequirement;
    private String credits;
    private Integer creditsUsedForGrad;
    private boolean projected;

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseLevel() {
        return courseLevel;
    }

    public void setCourseLevel(String courseLevel) {
        this.courseLevel = courseLevel;
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

    public String getCompletedCoursePercentage() {
        return completedCoursePercentage;
    }

    public void setCompletedCoursePercentage(String completedCoursePercentage) {
        this.completedCoursePercentage = completedCoursePercentage;
    }

    public String getCompletedCourseLetterGrade() {
        return completedCourseLetterGrade;
    }

    public void setCompletedCourseLetterGrade(String completedCourseLetterGrade) {
        this.completedCourseLetterGrade = completedCourseLetterGrade;
    }

    public String getBestSchoolPercent() {
        return bestSchoolPercent;
    }

    public void setBestSchoolPercent(String bestSchoolPercent) {
        this.bestSchoolPercent = bestSchoolPercent;
    }

    public String getBestExamPercent() {
        return bestExamPercent;
    }

    public void setBestExamPercent(String bestExamPercent) {
        this.bestExamPercent = bestExamPercent;
    }

    public String getInterimPercent() {
        return interimPercent;
    }

    public void setInterimPercent(String interimPercent) {
        this.interimPercent = interimPercent;
    }

    public String getEquivOrChallenge() {
        return equivOrChallenge;
    }

    public void setEquivOrChallenge(String equivOrChallenge) {
        this.equivOrChallenge = equivOrChallenge;
    }

    public String getMetLitNumRequirement() {
        return metLitNumRequirement;
    }

    public void setMetLitNumRequirement(String metLitNumRequirement) {
        this.metLitNumRequirement = metLitNumRequirement;
    }

    public boolean getProjected() {
        return projected;
    }

    public void setProjected(boolean projected) {
        this.projected = projected;
    }

    public String getCredits() {
        return credits;
    }

    public void setCredits(String credits) {
        this.credits = credits;
    }

    public Integer getCreditsUsedForGrad() {
        return creditsUsedForGrad;
    }

    public void setCreditsUsedForGrad(Integer creditsUsedForGrad) {
        this.creditsUsedForGrad = creditsUsedForGrad;
    }
}
