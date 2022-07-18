/*
 * *********************************************************************
 *  Copyright (c) 2017, Ministry of Education, BC.
 *
 *  All rights reserved.
 *    This information contained herein may not be used in whole
 *    or in part without the express written consent of the
 *    Government of British Columbia, Canada.
 *
 *  Revision Control Information
 *  File:                $Id::                                                 $
 *  Date of Last Commit: $Date::                                               $
 *  Revision Number:     $Rev::                                                $
 *  Last Commit by:      $Author::                                             $
 *
 * ***********************************************************************
 */
package ca.bc.gov.educ.api.dataconversion.model.tsw.report;

import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

import static java.lang.Integer.parseInt;

@Data
public class AchievementCourse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String pen;
    private String courseName = "";
    private String courseCode = "";
    private String courseLevel = "";
    private String requirement = "";
    private String equivalency = "";
    private String sessionDate = "";
    private String schoolPercent = "";
    private String examPercent = "";
    private String finalPercent = "";
    private String credits = "";
    private String finalLetterGrade = "";
    private String courseType = "";
    private String interimMark = "";
    private String interimLetterGrade = "";
    private String relatedCourse = "";
    private String relatedLevel = "";
    private String usedForGrad = "0";

    private String gradReqMet;
    private String completedCoursePercentage;
    private String completedCourseLetterGrade;
    private String interimPercent = "";
    private String equivOrChallenge;
    private boolean projected;

    /**
     * Constructor method.
     *
     */
    public AchievementCourse() {
    }

    /**
     * Constructor method. Used by the JPQL to create an object from the
     * database entities.
     * @param crseCode
     * @param crseLevel
     * @param sessionDate
     */
    public AchievementCourse(
            final String crseCode,
            final String crseLevel,
            final String sessionDate) {
        this.courseCode = nullSafe(crseCode);
        this.courseLevel = nullSafe(crseLevel);
        this.sessionDate = nullSafe(sessionDate);
    }

    /**
     * Constructor method. Used by the JPQL to create an object from the
     * database entities.
     * @param crseCode
     * @param crseLevel
     * @param sessionDate
     */
    public AchievementCourse(
            final String crseName,
            final String crseCode,
            final String crseLevel,
            final String sessionDate,
            final String gradReqMet,
            final String completedCoursePercentage,
            final String completedCourseLetterGrade,
            final String interimPercent,
            final String equivOrChallenge,
            final Integer usedForGrad) {
        this.courseName = nullSafe(crseName);
        this.courseCode = nullSafe(crseCode);
        this.courseLevel = nullSafe(crseLevel);
        this.sessionDate = nullSafe(sessionDate);

        this.gradReqMet = nullSafe(gradReqMet);
        this.completedCoursePercentage = nullSafe(completedCoursePercentage);
        this.completedCourseLetterGrade = nullSafe(completedCourseLetterGrade);
        this.interimPercent = nullSafe(interimPercent);
        this.equivOrChallenge = nullSafe(equivOrChallenge);
        this.usedForGrad = nullSafe(usedForGrad).toString();
    }

    /**
     * Constructor method. Used by the JPQL to create an object from the
     * database entities.
     *  @param pen
     * @param courseName
     * @param crseCode
     * @param crseLevel
     * @param sessionDate
     * @param credits
     * @param examPercent
     * @param schoolPercent
     * @param finalPercent
     * @param finalLetterGrade
     * @param interimMark
     * @param requirement
     * @param specialCase
     * @param courseType
     */
    public AchievementCourse(
            final String pen,
            final String courseName,
            final String crseCode,
            final String crseLevel,
            final String sessionDate,
            final String credits,
            final String examPercent,
            final String schoolPercent,
            final String finalPercent,
            final String finalLetterGrade,
            final String interimMark,
            final String requirement,
            final String specialCase,
            final String courseType,
            final Integer usedForGrad) {
        this.pen = pen;
        this.courseName = nullSafe(courseName);
        this.courseCode = nullSafe(crseCode);
        this.courseLevel = nullSafe(crseLevel);
        this.sessionDate = nullSafe(sessionDate);
        this.credits = nullSafe(credits);
        this.examPercent = nullSafe(examPercent);
        this.schoolPercent = nullSafe(schoolPercent);
        this.finalLetterGrade = nullSafe(finalLetterGrade);
        this.interimMark = nullSafe(interimMark);
        this.requirement = nullSafe(requirement);
        this.equivalency = nullSafe(specialCase);
        this.courseType = nullSafe(courseType);
        this.usedForGrad = nullSafe(usedForGrad).toString();

        if (finalPercent == null || finalPercent.equals("---")) {
            this.finalPercent = "";
        } else {
            this.finalPercent = finalPercent.trim();
        }
    }

    /**
     * Constructor method. Used by the JPQL to create an object from the
     * database entities.
     *
     * @param studNo
     * @param courseName
     * @param crseCode
     * @param crseLevel
     * @param courseSession
     * @param numCredits
     * @param examPct
     * @param schoolPct
     * @param finalPct
     * @param finalLg
     * @param interimMark
     * @param foundationReq
     * @param specialCase
     * @param rptCrsType
     * @param interimLetterGrade
     */
    public AchievementCourse(String studNo, String courseName, String crseCode, String crseLevel, String courseSession, String numCredits, String examPct, String schoolPct, String finalPct, String finalLg, String interimMark, String foundationReq, String specialCase, String rptCrsType, String interimLetterGrade) {
        this(studNo, courseName, crseCode, crseLevel, courseSession, numCredits, examPct, schoolPct, finalPct, finalLg, interimMark, foundationReq, specialCase, rptCrsType, (Integer)null);
        this.interimLetterGrade = (interimLetterGrade == null ? "" : interimLetterGrade.trim());
    }

    public String getPen() {
        return pen;
    }

    public String getCourseName() {
        return courseName;
    }

    
    public String getCourseCode() {
        return courseCode;
    }

    
    public String getRequirement() {
        return requirement;
    }

    
    public String getEquivalency() {
        return equivalency;
    }

    
    public String getSessionDate() {
        return sessionDate;
    }

    
    public String getSchoolPercent() {
        return schoolPercent;
    }

    
    public String getExamPercent() {
        return examPercent;
    }

    
    public String getFinalPercent() {
        return finalPercent;
    }

    
    public String getCredits() {
        return credits;
    }

    
    public String getFinalLetterGrade() {
        return finalLetterGrade;
    }

    
    public String getCourseType() {
        return courseType;
    }

    
    public Boolean isExaminable() {
        final String ct = getCourseType();
        final Boolean result = "1".equals(ct);

        return result;
    }

    
    public String getCourseLevel() {
        return courseLevel;
    }

    
    public String getInterimMark() {
        return interimMark;
    }

    
    public String getInterimLetterGrade() {
        return interimLetterGrade;
    }

    
    public String getRelatedCourse() {
        return relatedCourse;
    }

    
    public String getGradReqMet() {
        return gradReqMet;
    }

    
    public String getCompletedCoursePercentage() {
        return completedCoursePercentage;
    }

    
    public String getCompletedCourseLetterGrade() {
        return completedCourseLetterGrade;
    }

    
    public String getInterimPercent() {
        return interimPercent;
    }

    
    public String getEquivOrChallenge() {
        return equivOrChallenge;
    }

    /**
     * set the related course value.
     * <p>
     * @param relatedCourse
     */
    public void setRelatedCourse(String relatedCourse) {
        this.relatedCourse = relatedCourse;
    }

    
    public String getRelatedLevel() {
        return relatedLevel;
    }

    /**
     * set the related course level.
     * <p>
     * @param relatedLevel
     */
    public void setRelatedLevel(String relatedLevel) {
        this.relatedLevel = relatedLevel;
    }

    
    public String getUsedForGrad() {
        if (usedForGrad == null)
            return "0";
        else
            return usedForGrad;
    }

    
    public Integer getCreditsUsedForGrad() {
        return Integer.valueOf(getUsedForGrad());
    }

    
    public boolean courseEquals(final AchievementCourse compareCourse) {

        boolean isEqual = ((!this.equals(compareCourse))
                && this.getCourseCode().equals(compareCourse.getCourseCode())
                && this.getCourseLevel().equals(compareCourse.getCourseLevel()));
        return isEqual;
    }

    
    public boolean compareCourse(final AchievementCourse compareCourse) {

        final int interimPercentage = getInt(this.getInterimMark());
        final int finalPercentage = getInt(this.getFinalPercent());
        final int compareFinalPercentage = getInt(compareCourse.getFinalPercent());
        final int compareInterimPercentage = getInt(compareCourse.getInterimMark());

        // Removes duplication of courses by comparing and finding course with
        //highest percentage.
        boolean replaceCourse = ((interimPercentage < compareFinalPercentage
                && finalPercentage < compareFinalPercentage
                && compareFinalPercentage != 0)
                || (finalPercentage < compareInterimPercentage
                && finalPercentage != 0
                && compareInterimPercentage != 0));
        return replaceCourse;
    }

    /**
     * Returns the integer value of the given string.
     *
     * @param s The string that contains an integer.
     * @return The integer value from the string, or 0 if no value found.
     */
    private int getInt(final String s) {
        int value = 0;
        if (s.matches("^-?\\d+$")) {
            value = parseInt(s);
        }
        return value;
    }

    /**
     * Set the code value which indicates if this course is used for graduation
     * requirements.
     *
     * @param creditsUsedForGrad
     */
    public void setCreditsUsedForGrad(final String creditsUsedForGrad) {
        this.usedForGrad = creditsUsedForGrad;
    }

    
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + (this.pen != null ? this.pen.hashCode() : 0);
        hash = 59 * hash + (this.courseCode != null ? this.courseCode.hashCode() : 0);
        hash = 59 * hash + (this.sessionDate != null ? this.sessionDate.hashCode() : 0);
        return hash;
    }

    /**
     * Returns a trimmed version of the given string.
     *
     * @param s The string to trim.
     * @return The empty string if s is null, otherwise s.trim().
     */
    private String nullSafe(final String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * Returns a version of the given Integer.
     *
     * @param s The string to trim.
     * @return The empty string if s is null, otherwise s.trim().
     */
    private Integer nullSafe(final Integer s) {
        return s == null ? 0 : s;
    }

    /**
     * Returns c or an empty space if c is null.
     *
     * @param c The character to ensure is not null.
     * @return A space or the given character, never null.
     */
    private String nullSafe(final Character c) {
        return c == null ? " " : c.toString();
    }

    
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AchievementCourse other = (AchievementCourse) obj;
        if (!Objects.equals(this.pen, other.pen)) {
            return false;
        }
        if (!Objects.equals(this.courseName, other.courseName)) {
            return false;
        }
        if (!Objects.equals(this.courseCode, other.courseCode)) {
            return false;
        }
        if (!Objects.equals(this.courseLevel, other.courseLevel)) {
            return false;
        }
        if (!Objects.equals(this.requirement, other.requirement)) {
            return false;
        }
        if (!Objects.equals(this.equivalency, other.equivalency)) {
            return false;
        }
        if (!Objects.equals(this.sessionDate, other.sessionDate)) {
            return false;
        }
        if (!Objects.equals(this.schoolPercent, other.schoolPercent)) {
            return false;
        }
        if (!Objects.equals(this.examPercent, other.examPercent)) {
            return false;
        }
        if (!Objects.equals(this.finalPercent, other.finalPercent)) {
            return false;
        }
        if (!Objects.equals(this.credits, other.credits)) {
            return false;
        }
        if (!Objects.equals(this.finalLetterGrade, other.finalLetterGrade)) {
            return false;
        }
        if (!Objects.equals(this.courseType, other.courseType)) {
            return false;
        }
        if (!Objects.equals(this.interimMark, other.interimMark)) {
            return false;
        }
        if (!Objects.equals(this.interimLetterGrade, other.interimLetterGrade)) {
            return false;
        }
        if (!Objects.equals(this.relatedCourse, other.relatedCourse)) {
            return false;
        }
        if (!Objects.equals(this.relatedLevel, other.relatedLevel)) {
            return false;
        }
        return Objects.equals(this.usedForGrad, other.usedForGrad);
    }

}
